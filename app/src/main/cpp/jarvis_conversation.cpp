#include <jni.h>
#include "llama.h"
#include "ggml-backend.h"
#include <algorithm>
#include <atomic>
#include <cstdint>
#include <memory>
#include <mutex>
#include <stdexcept>
#include <string>
#include <thread>
#include <vector>

namespace {
std::mutex model_mutex;
std::once_flag backend_once;
std::atomic<int64_t> active_request{-1};
llama_model * model = nullptr;
std::string loaded_path;
struct Request { int64_t id; };
bool aborted(void * data) { return active_request.load() != static_cast<Request *>(data)->id; }
bool loading(float, void * data) { return !aborted(data); }
void throw_error(JNIEnv * env, const char * message) {
    env->ThrowNew(env->FindClass("java/lang/IllegalStateException"), message);
}
}

extern "C" JNIEXPORT void JNICALL
Java_br_com_jarvis_mobile_conversation_LlamaBridge_activate(JNIEnv *, jobject, jlong id) {
    active_request.store(id);
}
extern "C" JNIEXPORT void JNICALL
Java_br_com_jarvis_mobile_conversation_LlamaBridge_cancel(JNIEnv *, jobject) {
    active_request.store(-1);
}
extern "C" JNIEXPORT void JNICALL
Java_br_com_jarvis_mobile_conversation_LlamaBridge_release(JNIEnv *, jobject) {
    std::lock_guard<std::mutex> lock(model_mutex);
    if (model != nullptr) { llama_model_free(model); model = nullptr; loaded_path.clear(); }
}

extern "C" JNIEXPORT jbyteArray JNICALL
Java_br_com_jarvis_mobile_conversation_LlamaBridge_generate(
        JNIEnv * env, jobject, jstring jpath, jbyteArray jprompt, jlong id, jint limit) {
    try {
        std::lock_guard<std::mutex> lock(model_mutex);
        Request request{id};
        if (aborted(&request)) return env->NewByteArray(0);
        std::call_once(backend_once, [] {
            llama_backend_init();
            ggml_backend_load_all();
            llama_log_set([](ggml_log_level, const char *, void *) {}, nullptr);
        });
        const char * path_chars = env->GetStringUTFChars(jpath, nullptr);
        if (!path_chars) return nullptr;
        std::string path(path_chars);
        env->ReleaseStringUTFChars(jpath, path_chars);
        const jsize byte_count = env->GetArrayLength(jprompt);
        if (byte_count <= 0 || byte_count > 16000) throw std::runtime_error("PROMPT_INVALID");
        std::string prompt(byte_count, '\0');
        env->GetByteArrayRegion(jprompt, 0, byte_count, reinterpret_cast<jbyte *>(prompt.data()));
        if (model == nullptr || loaded_path != path) {
            if (model != nullptr) { llama_model_free(model); model = nullptr; loaded_path.clear(); }
            auto params = llama_model_default_params();
            params.n_gpu_layers = 0;
            params.use_mmap = true;
            params.use_mlock = false;
            params.progress_callback = loading;
            params.progress_callback_user_data = &request;
            model = llama_model_load_from_file(path.c_str(), params);
            if (model == nullptr) {
                if (aborted(&request)) return env->NewByteArray(0);
                throw std::runtime_error("MODEL_LOAD_FAILED");
            }
            loaded_path = path;
        }
        const auto * vocab = llama_model_get_vocab(model);
        int count = -llama_tokenize(vocab, prompt.data(), prompt.size(), nullptr, 0, false, true);
        const int max_tokens = std::clamp<int>(limit, 1, 192);
        if (count <= 0 || count + max_tokens > 2048) throw std::runtime_error("CONTEXT_TOO_LONG");
        std::vector<llama_token> tokens(count);
        if (llama_tokenize(vocab, prompt.data(), prompt.size(), tokens.data(), count, false, true) != count)
            throw std::runtime_error("TOKENIZATION_FAILED");
        auto params = llama_context_default_params();
        params.n_ctx = 2048;
        params.n_batch = 2048;
        params.n_ubatch = 128;
        params.n_threads = std::clamp<int>(std::thread::hardware_concurrency(), 1, 4);
        params.n_threads_batch = params.n_threads;
        params.offload_kqv = false;
        params.abort_callback = aborted;
        params.abort_callback_data = &request;
        std::unique_ptr<llama_context, decltype(&llama_free)> ctx(llama_init_from_model(model, params), llama_free);
        if (!ctx) throw std::runtime_error("CONTEXT_LOAD_FAILED");
        std::unique_ptr<llama_sampler, decltype(&llama_sampler_free)> sampler(
            llama_sampler_chain_init(llama_sampler_chain_default_params()), llama_sampler_free);
        if (!sampler) throw std::runtime_error("SAMPLER_FAILED");
        llama_sampler_chain_add(sampler.get(), llama_sampler_init_top_k(30));
        llama_sampler_chain_add(sampler.get(), llama_sampler_init_top_p(0.90f, 1));
        llama_sampler_chain_add(sampler.get(), llama_sampler_init_temp(0.45f));
        llama_sampler_chain_add(sampler.get(), llama_sampler_init_dist(42));
        auto batch = llama_batch_get_one(tokens.data(), count);
        std::string answer;
        for (int i = 0; i < max_tokens && !aborted(&request); ++i) {
            if (llama_decode(ctx.get(), batch) != 0) {
                if (aborted(&request)) return env->NewByteArray(0);
                throw std::runtime_error("DECODE_FAILED");
            }
            llama_token next = llama_sampler_sample(sampler.get(), ctx.get(), -1);
            if (llama_vocab_is_eog(vocab, next)) break;
            std::vector<char> piece(256);
            int length = llama_token_to_piece(vocab, next, piece.data(), piece.size(), 0, false);
            if (length < 0) { piece.resize(-length); length = llama_token_to_piece(vocab, next, piece.data(), piece.size(), 0, false); }
            if (length < 0) throw std::runtime_error("TOKEN_DECODE_FAILED");
            answer.append(piece.data(), length);
            if (answer.size() > 2400) break;
            tokens.assign(1, next);
            batch = llama_batch_get_one(tokens.data(), 1);
        }
        if (aborted(&request)) return env->NewByteArray(0);
        auto result = env->NewByteArray(answer.size());
        if (result) env->SetByteArrayRegion(result, 0, answer.size(), reinterpret_cast<const jbyte *>(answer.data()));
        return result;
    } catch (const std::bad_alloc &) { throw_error(env, "NOT_ENOUGH_MEMORY"); }
      catch (const std::exception & error) { throw_error(env, error.what()); }
    return nullptr;
}
