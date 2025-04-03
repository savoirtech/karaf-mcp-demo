# Apache Karaf MCP Demo:


This project demonstrates Apache Karaf, LangChain4j, MCP client integration.

A separate MCP Server is required to fully use this demo.

## Design

 Ollama: LLM host.

 Karaf: MCP Host. Contains our main application, with LLM integration via LangChain4j, and MCP Client to MCP Server.

 MCP Server: Implemented with Spring AI MCP Server.


## Build

```
mvn clean install
```

## Deploy

Start Apache Karaf 4.4.7, then perform the following commands:

```text
karaf@root()> feature:repo-add mvn:com.savoir.mcp.demo/BookStoreFeature/1.0.0-SNAPSHOT/xml/features
karaf@root()> feature:install mcp-demo-all
```


## MCP Server

We have implemented a Spring AI MCP Server here:

https://github.com/savoirtech/spring-ai-mcp-server-demo
