/*
 * Copyright (c) 2012-2025 Savoir Technologies, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.savoir.mcp.demo.bookstore.impl;

import com.savoir.mcp.demo.bookstore.api.SavoirBooks;
import com.savoir.mcp.demo.bookstore.api.Book;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;

import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.http.HttpMcpTransport;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.tool.ToolProvider;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.function.Supplier;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = SavoirBooks.class)
public class SavoirBooksImpl implements SavoirBooks {

    private static final Logger logger = LoggerFactory.getLogger(SavoirBooksImpl.class);

    ChatLanguageModel model;
    SavoirBot bot;
    McpTransport transport;
    final static String MODEL_NAME = "mistral";
    final static String BASE_URL = "http://127.0.0.1:11434";


    public SavoirBooksImpl() {
        model = OllamaChatModel.builder()
                .baseUrl(BASE_URL)
                .modelName(MODEL_NAME)
                .temperature(0.2) // Low temperature for deterministic, fact-based answers
                .timeout(Duration.ofSeconds(60))
                .logRequests(true)
                .logResponses(true)
                .build();

        transport = new HttpMcpTransport.Builder()
                .sseUrl("http://localhost:3001/sse")
                .logRequests(true)
                .logResponses(true)
                .timeout(Duration.ofSeconds(60))
                .build();

        McpClient mcpClient = new DefaultMcpClient.Builder()
                .transport(transport)
                .build();

        ToolProvider toolProvider = McpToolProvider.builder()
                .mcpClients(List.of(mcpClient))
                .build();

        // Define a system message to  instruct the model on how it should behave throughout the session.
        Function<Object, String> functionSystemMessageProvider = (ignored) ->
                "You are a truthful assistant. If you don't know something, say 'I don't know' instead of making up an answer. " +
                "Do not tell users about functions to obtain answer to query - execute the function. " +
                "If you provide an answer, explain how you know or where the information came from. If unsure, say so. " +
                "If an API request fails then inform the user of the error, do not try other methods. ";

        // Define a custom hallucination strategy
        Function<ToolExecutionRequest, ToolExecutionResultMessage> customStrategy = request -> {
            return ToolExecutionResultMessage.from(request, "Error: the tool '" + request.name() + "' does not exist.");
        };

        bot = AiServices.builder(SavoirBot.class)
                .chatLanguageModel(model)
                .systemMessageProvider(functionSystemMessageProvider)
                .hallucinatedToolNameStrategy(customStrategy)
                .toolProvider(toolProvider)
                .build();
    }

    @Override
    public List<Book> getAllBooks() {
        return List.of();
    }

    @Override
    public Book getBook(String s) {
        return null;
    }

    @Override
    public List<Book> getBooksByAuthor(String s) {
        return List.of();
    }

    @Override
    public List<Book> getBooksByTitle(String s) {
        return List.of();
    }

    @Override
    public void addBookToStore(Book book) {

    }

    @Override
    public void removeBookFromStore(Book book) {

    }

    @Override
    public String ask(String question) {

        // Configure the TimeLimiter
        TimeLimiterConfig config = TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofSeconds(8))  // max time allowed
                .cancelRunningFuture(true)
                .build();

        // Add to registry
        TimeLimiterRegistry registry = TimeLimiterRegistry.of(config);

        // Create time limiter with name.
        TimeLimiter timeLimiter = registry.timeLimiter("aiAskTimeLimiter");

        // Executor for async work
        ExecutorService executorService = Executors.newSingleThreadExecutor();

        // Potentially long-running task
        Callable<String> aiTask = () -> {
            try {
                // Our opportunity to use tools, such as MCP.
                return bot.ask(question);
            } catch (Exception e) {
                logger.error(e.getMessage());
                return e.getMessage();
            }
        };

        String response = null;
        // Wrap the task in a CompletableFuture
        Supplier<CompletableFuture<String>> futureSupplier = () ->
                CompletableFuture.supplyAsync(() -> {
                    try {
                        return aiTask.call();
                    } catch (Exception e) {
                        throw new CompletionException(e);
                    }
                }, executorService);

        try {
            response = timeLimiter.executeFutureSupplier(futureSupplier);
        } catch (TimeoutException e) {
            logger.error("Timed out! " + e.getMessage());
            response = e.getMessage();
        } catch (Exception e) {
            logger.error(e.getMessage());
            response = e.getMessage();
        } finally {
            executorService.shutdownNow();
        }

        return response;
    }
}
