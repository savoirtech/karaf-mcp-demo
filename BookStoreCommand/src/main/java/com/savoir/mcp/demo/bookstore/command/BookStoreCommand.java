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
package com.savoir.mcp.demo.bookstore.command;

import com.savoir.mcp.demo.bookstore.api.SavoirBooks;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;

@Service
@Command(scope = "ai", name = "ask", description = "Ask SavoirBooks ai a question.")
public class BookStoreCommand implements Action {

    @Reference
    private SavoirBooks bookStore;

    @Argument(index = 0, name = "question", description = "User question", required = true, multiValued = false)
    String question;

    @Override
    public Object execute() throws Exception {
        String response = bookStore.ask(question);
        System.out.println(response);
        return null;
    }

}
