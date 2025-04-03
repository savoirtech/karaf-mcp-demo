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

import dev.langchain4j.model.chat.ChatLanguageModel;

import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.http.HttpMcpTransport;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.tool.ToolProvider;
import java.time.Duration;
import java.util.List;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;

@Component(service = SavoirBooks.class)
public class SavoirBooksImpl implements SavoirBooks {

    ChatLanguageModel model;
    SavoirBot bot;
    McpTransport transport;
    final static String MODEL_NAME = "mistral";
    final static String BASE_URL = "http://127.0.0.1:11434";


    public SavoirBooksImpl() {
        model = OllamaChatModel.builder()
                .baseUrl(BASE_URL)
                .modelName(MODEL_NAME)
                .temperature(0.8)
                .timeout(Duration.ofSeconds(60))
                .logRequests(true)
                .logResponses(true)
                .build();

        transport = new HttpMcpTransport.Builder()
                .sseUrl("http://localhost:3001/sse")
                .logRequests(true)
                .logResponses(true)
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
        McpClient mcpClient = new DefaultMcpClient.Builder()
                .transport(transport)
                .build();

        ToolProvider toolProvider = McpToolProvider.builder()
                .mcpClients(List.of(mcpClient))
                .build();

        bot = AiServices.builder(SavoirBot.class)
                .chatLanguageModel(model)
                .toolProvider(toolProvider)
                .build();

        String response = "";
        try {
            // Our opportunity to use tools, such as MCP.
            response = bot.ask(question);
        } catch (Exception e) {
            System.out.println("ERROR: " + e.getMessage());
        }
        return response;
    }

    @Deactivate
    void deactivate() {
        try {
            transport.close();
        } catch (Exception e) {
            //Log Error
        }
    }
}
