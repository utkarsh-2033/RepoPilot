# RepoPilot Backend

> AI-powered codebase assistant backend that connects GitHub repositories, indexes source code into a vector store, and answers repository-specific questions with streaming responses and exact file/line citations.

RepoPilot is designed around one core principle: **the AI should answer from the codebase, not from guesses**.

This repository contains the Spring Boot backend responsible for authentication, GitHub integration, repository synchronization, asynchronous indexing, semantic retrieval, LLM generation, citation validation, and chat persistence.

---

## ✨ What RepoPilot Does

RepoPilot lets a user:

1. Sign in with GitHub using OAuth 2.0.
2. Synchronize their GitHub repositories.
3. Select a repository and start indexing it.
4. Fetch eligible source files from the repository.
5. Split files into token-sized chunks and generate embeddings through Spring AI's configured `VectorStore`.
6. Store repository-scoped vectors with metadata such as file path, language, and chunk position.
7. Ask questions about the repository.
8. Retrieve the most relevant code chunks using semantic similarity search.
9. Send the retrieved context to an LLM through Spring AI.
10. Receive the answer as a **Server-Sent Events (SSE)** stream.
11. Validate the citations emitted by the model against the actually retrieved sources.
12. Persist the conversation and its validated citations for later retrieval.

The result is a **repository-grounded RAG pipeline** rather than a generic chatbot.

---

## 🏗️ Architecture

```text
                         ┌─────────────────────┐
                         │      Frontend       │
                         │     Next.js App     │
                         └──────────┬──────────┘
                                    │
                         HTTP / SSE │
                                    ▼
┌─────────────────────────────────────────────────────────────────┐
│                       Spring Boot Backend                       │
│                                                                 │
│  ┌───────────────┐      ┌────────────────────────────────────┐  │
│  │ OAuth /       │      │ Repository Management               │  │
│  │ Spring        │      │                                    │  │
│  │ Security      │      │ sync → persist → index → status    │  │
│  └───────┬───────┘      └────────────────┬───────────────────┘  │
│          │                                │                      │
│          │                                ▼                      │
│          │                   ┌──────────────────────────┐       │
│          │                   │      GitHub API          │       │
│          │                   │ repositories / tree /    │       │
│          │                   │ file contents            │       │
│          │                   └─────────────┬────────────┘       │
│          │                                 │                     │
│          │                                 ▼                     │
│          │                   ┌──────────────────────────┐       │
│          │                   │       File Filter        │       │
│          │                   │ allowed extensions +     │       │
│          │                   │ ignored directories      │       │
│          │                   └─────────────┬────────────┘       │
│          │                                 │                     │
│          │                                 ▼                     │
│          │                   ┌──────────────────────────┐       │
│          │                   │     Token Chunking       │       │
│          │                   │ Spring AI TokenText      │       │
│          │                   │ Splitter                 │       │
│          │                   └─────────────┬────────────┘       │
│          │                                 │                     │
│          │                                 ▼                     │
│          │                   ┌──────────────────────────┐       │
│          │                   │       VectorStore        │       │
│          │                   │ embeddings + metadata    │       │
│          │                   └─────────────┬────────────┘       │
│          │                                 │                     │
│          │             User Question       │                     │
│          │                                 ▼                     │
│          │                   ┌──────────────────────────┐       │
│          │                   │       Retrieval           │       │
│          │                   │ similarity search +       │       │
│          │                   │ repoId filter + Top-K     │       │
│          │                   └─────────────┬────────────┘       │
│          │                                 │                     │
│          │                                 ▼                     │
│          │                   ┌──────────────────────────┐       │
│          │                   │      Prompt Builder      │       │
│          │                   │ code context + grounding │       │
│          │                   │ + citation instructions  │       │
│          │                   └─────────────┬────────────┘       │
│          │                                 │                     │
│          │                                 ▼                     │
│          │                   ┌──────────────────────────┐       │
│          │                   │       ChatClient         │       │
│          │                   │       Spring AI          │       │
│          │                   └─────────────┬────────────┘       │
│          │                                 │ Flux<String>        │
│          │                                 ▼                     │
│          │                   ┌──────────────────────────┐       │
│          │                   │      SSE Stream           │       │
│          │                   │ token → citations → done  │       │
│          │                   └─────────────┬────────────┘       │
│          │                                 │                     │
│          │                                 ▼                     │
│          │                   ┌──────────────────────────┐       │
│          │                   │ Citation Validator        │       │
│          │                   │ only accepts retrieved    │       │
│          │                   │ citation IDs              │       │
│          │                   └──────────────────────────┘       │
│                                                                 │
│                     JPA / PostgreSQL persistence                │
└─────────────────────────────────────────────────────────────────┘
```

---

## 🔄 RAG Pipeline

The core question-answering flow is:

```text
User Question
     │
     ▼
Chat Session Authorization
     │
     ▼
Retrieve Top-K Chunks
     │
     │  metadata filter:
     │  repoId = current repository
     ▼
Semantic Similarity Search
     │
     ▼
Build Code Context
     │
     ▼
Grounded System Prompt
     │
     ▼
Spring AI ChatClient
     │
     ▼
LLM Token Stream
     │
     ├──────────────► SSE: token
     │
     ▼
Full Answer
     │
     ▼
Extract [C1], [C2], ...
     │
     ▼
Validate Against Retrieved Sources
     │
     ├──────────────► SSE: citations
     │
     └──────────────► Persist assistant message
                         │
                         ▼
                      SSE: done
```

### Why repository filtering matters

Every indexed chunk contains a `repoId` metadata field.

During retrieval, RepoPilot applies:

```text
repoId == current repository
```

before semantic search results are returned.

This prevents a question about one repository from retrieving chunks belonging to another indexed repository.

---

## 📚 Indexing Pipeline

Repository indexing runs asynchronously using a dedicated Spring `ThreadPoolTaskExecutor`.

```text
Start Indexing
     │
     ▼
Validate User + Repository
     │
     ▼
Set status = INDEXING
     │
     ▼
@Async indexing task
     │
     ▼
Delete previous vectors for repository
     │
     ▼
Fetch GitHub recursive repository tree
     │
     ▼
Filter eligible files
     │
     ▼
Fetch file contents
     │
     ▼
Decode Base64 content when necessary
     │
     ▼
Split into token chunks
     │
     ▼
Attach metadata
     │
     ▼
Batch vector insertion
     │
     ▼
Update progress
     │
     ▼
status = READY
```

If indexing fails, the repository is marked `FAILED` and an error message is stored.

### Indexed metadata

Each vector chunk carries metadata including:

| Metadata | Purpose |
|---|---|
| `repoId` | Repository isolation during retrieval |
| `filePath` | Source file for citation |
| `language` | Language information for the UI/LLM |
| `chunkIndex` | Position of the chunk |
| `startLine` | Approximate source start line |
| `endLine` | Approximate source end line |

---

## 🧹 File Selection Strategy

RepoPilot intentionally does not embed every file in a repository.

### Ignored directories

Examples include:

- `node_modules`
- `.git`
- `dist`
- `build`
- `target`
- `.next`
- `vendor`
- `__pycache__`
- `.idea`
- `.vscode`
- `coverage`
- `out`

### Ignored files

Examples:

- `package-lock.json`
- `yarn.lock`
- `pnpm-lock.yaml`
- `composer.lock`
- `cargo.lock`
- `poetry.lock`

Hidden files are also excluded.

---

## 🔐 Authentication & Authorization

RepoPilot uses **GitHub OAuth 2.0** through Spring Security.

The OAuth flow:

```text
User
 │
 ▼
GitHub OAuth
 │
 ▼
Spring Security OAuth2
 │
 ▼
CustomOAuth2UserService
 │
 ├── register/update local user
 └── store GitHub access token
 │
 ▼
AppUserPrincipal
 │
 ▼
Authenticated API requests
```

The backend uses the authenticated principal to associate repositories and chat sessions with the current user.

GitHub API requests are made through Spring's OAuth2 authorized client infrastructure, allowing the GitHub access token to be attached to API requests.

---

## 💬 Chat & Streaming

Chat responses are streamed using **Server-Sent Events (SSE)**.

The complete assistant response is assembled server-side while the tokens are streamed to the client.

Once generation completes, the backend:

1. extracts citation IDs from the answer,
2. validates those IDs against the retrieved documents,
3. stores the assistant message,
4. stores only validated citations,
5. sends the citation event,
6. sends the final `done` event.

---

## 🎯 Citation Grounding

Citation support is a first-class part of the RAG implementation.

Retrieved chunks are assigned IDs:

```text
[C1] RepositoryController.java
[C2] IndexingService.java
[C3] Retrieval.java
```

The prompt instructs the model to reference these IDs when making codebase claims.

For example:

```text
Repository indexing is started asynchronously [C2].
```

The `CitationValidator` parses citations using:

```text
[C1]
[C2]
[C3]
...
```

and maps them back to the retrieved `CitationDto` objects.

A citation is persisted only if its ID exists in the current retrieval result.

This provides an important guardrail against the model inventing arbitrary file citations.

---

## 🗄️ Persistence Model

The backend uses JPA repositories for application data.

### User

Stores GitHub identity and OAuth-related information.

```text
User
 ├── id
 ├── provider
 ├── providerId
 ├── username
 ├── email
 ├── name
 ├── avatarUrl
 └── accessToken
```

### Repository

Stores synchronized GitHub repository metadata and indexing state.

```text
Repository
 ├── id
 ├── userId
 ├── githubRepoId
 ├── owner
 ├── name
 ├── fullName
 ├── defaultBranch
 ├── language
 ├── isPrivate
 ├── indexStatus
 ├── filesProcessed
 ├── filesTotal
 ├── chunkCount
 └── errorMessage
```

### ChatSession

Represents a conversation scoped to a repository.

```text
ChatSession
 ├── id
 ├── userId
 ├── repositoryId
 ├── title
 └── updatedAt
```

### ChatMessage

Stores both user and assistant messages.

```text
ChatMessage
 ├── id
 ├── sessionId
 ├── message
 ├── role
 ├── citations (JSONB)
 └── createdAt
```

---

## 🌐 API Reference

### Authentication

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/login_url` | Returns the GitHub OAuth authorization path |
| `GET` | `/api/user/me` | Returns the authenticated user |

### Repository APIs

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/user/repos` | Get repositories synchronized for the current user |
| `GET` | `/api/user/repo/{githubRepoId}` | Get one repository |
| `POST` | `/api/user/repos/sync` | Fetch and persist the user's GitHub repositories |
| `POST` | `/api/user/repo/{id}/index` | Start asynchronous repository indexing |
| `GET` | `/api/user/repo/{id}/status` | Get indexing status/progress |

### Chat APIs

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/chat/session/init/{repoId}` | Create a chat session for a repository |
| `GET` | `/api/chat/sessions` | Get the user's chat sessions |
| `GET` | `/api/chat/session/{sessionId}/messages` | Get messages for a session |
| `DELETE` | `/api/chat/session/{sessionId}` | Delete a chat session |
| `POST` | `/api/chat/session/{sessionId}/message` | Ask a repository question and receive an SSE stream |

Swagger/OpenAPI endpoints are also exposed:

```text
/swagger-ui
/swagger-ui.html
/v3/api-docs
```

---

## 🧩 Project Structure

```text
backend/
├── BackendApplication.java
│
├── config/
│   ├── AsyncConfig.java
│   ├── ChatClientConfig.java
│   ├── CorsConfig.java
│   ├── RestClientConfig.java
│   ├── SecurityConfig.java
│   └── TokenSplitterConfig.java
│
├── controller/
│   ├── ChatController.java
│   ├── RepositoryController.java
│   └── UserController.java
│
├── dto/
│   ├── ChatMessageResponseDto.java
│   ├── ChatRequestDto.java
│   ├── CitationDto.java
│   ├── GithubRepository.java
│   ├── GithubRepositoryResponse.java
│   ├── GithubRepoOwner.java
│   ├── RepositoryIndexStatusResponse.java
│   ├── SessionsResponseDto.java
│   └── UserResponse.java
│
├── entity/
│   ├── ChatMessage.java
│   ├── ChatSession.java
│   ├── DocumentMetaData.java
│   ├── IndexStatus.java
│   ├── RepoFile.java
│   ├── Repository.java
│   └── User.java
│
├── exception/
│   ├── BadRequestException.java
│   ├── GithubApiClientException.java
│   ├── GlobalExceptionHandler.java
│   ├── RepositoryNotFoundException.java
│   └── SessionNotFoundException.java
│
├── repository/
│   ├── ChatMessageRepo.java
│   ├── ChatRepo.java
│   ├── RepositoryRepo.java
│   └── UserRepo.java
│
└── service/
    ├── ChatService.java
    ├── RepoService.java
    ├── RepositoryPersistenceService.java
    ├── UserService.java
    │
    ├── auth/
    │   ├── AppUserPrincipal.java
    │   └── CustomOAuth2UserService.java
    │
    ├── chat/
    │   ├── ChatStreamHandler.java
    │   └── ChatStreamResult.java
    │
    ├── citation/
    │   ├── CitationMapper.java
    │   └── CitationValidator.java
    │
    ├── github/
    │   ├── GithubApiClient.java
    │   └── GithubApiRateLimiter.java
    │
    ├── indexing/
    │   ├── FileChunking.java
    │   ├── FileFilter.java
    │   └── IndexingService.java
    │
    └── rag/
        ├── PromptBuilder.java
        └── Retrieval.java
```

---

## ⚙️ Configuration

The backend is designed so model, embedding, vector-store, chunking, retrieval, and deployment configuration can be supplied through Spring configuration/environment variables.

Important application-level properties used by the code include:

```properties
app.github.api-delay-ms=50
app.indexing.chunk-size=800
indexing.max-file-bytes=102400
retrieval.top-k=8
```

---

## 🧠 Spring AI Design

RepoPilot intentionally keeps the AI layer behind Spring AI abstractions.

The backend uses:

- `ChatClient` for LLM interaction
- `ChatModel` for model/provider abstraction
- `VectorStore` for semantic retrieval
- `Document` for indexed chunks
- `TokenTextSplitter` for chunking

```

This keeps the application logic focused on **RAG orchestration** rather than provider-specific API calls.

---

## 🚀 Running Locally


### 1. Clone the repository

```bash
git clone <your-repository-url>
cd <repository-directory>
```

### 2. Configure environment variables

Create your local environment configuration with:

```env
DB_URL=...
DB_USERNAME=...
DB_PASSWORD=...

GITHUB_CLIENT_ID=...
GITHUB_CLIENT_SECRET=...

OPENAI_API_KEY=...
```

Add the required embedding/vector-store configuration for your chosen Spring AI setup.

### 3. Start the backend

Using Maven:

```bash
./mvnw spring-boot:run
```
---

## 🧪 Typical Usage Flow

### 1. Login

```text
GET /login_url
```

Authenticate through GitHub.

### 2. Sync repositories

```http
POST /api/user/repos/sync
```

The backend fetches repositories from GitHub and persists their metadata.

### 3. View repositories

```http
GET /api/user/repos
```

### 4. Index a repository

```http
POST /api/user/repo/{githubRepoId}/index
```

The endpoint returns immediately while indexing runs asynchronously.

### 5. Track progress

```http
GET /api/user/repo/{githubRepoId}/status
```

Possible states:

```text
PENDING
INDEXING
READY
FAILED
```

### 6. Create a chat session

```http
POST /api/chat/session/init/{repoId}
```

### 7. Ask a question

```http
POST /api/chat/session/{sessionId}/message
```

Example:

```json
{
  "question": "Explain how repository indexing works."
}
```

The response is streamed over SSE with tokens, validated citations, and a completion event.

---

## ⚡ Concurrency & Indexing

Indexing is explicitly separated from the HTTP request thread.

The backend defines an executor:

```text
core pool: 5
max pool: 10
queue: 25
thread prefix: Indexing-
```

This allows repository indexing to continue in the background while the API returns an accepted response.

Vector insertion is also batched:

```text
VECTOR_BATCH_SIZE = 32
```

This avoids issuing a vector-store operation for every individual chunk.

---

## 🛡️ Error Handling

The backend defines application-specific exceptions including:

- `BadRequestException`
- `RepositoryNotFoundException`
- `SessionNotFoundException`
- `GithubApiClientException`

A global exception handler centralizes API error responses.

---

## 🔌 External Services

RepoPilot's backend integrates with:

- **GitHub** — OAuth authentication and repository/file access
- **PostgreSQL** — application persistence
- **Spring AI** — AI abstraction layer
- **LLM provider** — answer generation
- **Embedding provider** — vector generation
- **Vector store** — semantic code retrieval

The concrete LLM, embedding, and vector-store implementations can be configured independently of the core RAG orchestration.

---

## ⭐ RepoPilot

**RepoPilot turns a GitHub repository into a searchable, conversational codebase.**

Instead of asking an LLM to guess how a project works, RepoPilot retrieves the relevant code, grounds the response in that context, streams the answer, and exposes validated source citations.

```text
GitHub Repository
       ↓
   Index Code
       ↓
    Embeddings
       ↓
   Vector Store
       ↓
 Semantic Retrieval
       ↓
   Grounded LLM
       ↓
 Streaming Answer
       ↓
  Source Citations
```
