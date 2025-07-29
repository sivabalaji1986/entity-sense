# EntitySense

A Spring Boot 3.5.4 application that uses vector similarity search (via `pgvector` + `Ollama`) to detect potential **sanctioned**, **mule**, or other **high-risk entities** during payment processing.

---

## Tech Stack

| Tool               | Purpose                                  |
|--------------------|------------------------------------------|
| Java 21            | Application runtime                      |
| Spring Boot 3.5.4  | Web backend framework                    |
| PostgreSQL 17.5+   | Database with pgvector extension         |
| pgvector           | Vector storage and similarity search     |
| Ollama             | Local embedding model (`nomic-embed-text`) |
| Swagger (SpringDoc)| API documentation                       |
| WebClient          | HTTP client for embedding requests       |

---

## Features

- REST API to validate a payee against known high-risk entities
- Uses vector embeddings + cosine similarity for fuzzy matching
- Risk categories supported:
  - `SANCTION`, `MULE`, `MONEY_LAUNDERING`, `CYBER_THREAT`, `SHELL_ENTITY`, `PEP`, `SCAM_ENTITY`
- Ollama integration for real-time embedding generation
- Swagger UI for API exploration

---

## API Endpoints

| Endpoint                       | Method | Description                                                  |
|--------------------------------|--------|--------------------------------------------------------------|
| `/api/validate-payment`        | POST   | Validates if a payee is a potential match to watchlist entities |
| `/api/create-watch-list-entity` | POST   | Adds a new high-risk entity to the database                 |

> 🔍 Swagger UI: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)

---

## Prerequisites

| Tool     | Setup |
|----------|-------|
| PostgreSQL 17.5+ | `brew install postgresql@17` |
| pgvector         | `brew install pgvector` |
| Ollama           | `curl -fsSL https://ollama.com/install.sh | sh` |
| Java 21          | Ensure `JAVA_HOME` is set |
| Maven 3.9+       | Use IntelliJ or CLI |
| DBeaver (Optional) | For DB inspection |
| Postman / curl (Optional) | For testing APIs |

---

## Database Setup

```sql

psql postgres

CREATE USER entityadmin WITH ENCRYPTED PASSWORD '<<ENTITY_PASSWORD_TO_BE_USED>>';

-- Enable vector support
CREATE EXTENSION IF NOT EXISTS vector;

ALTER ROLE entityadmin CREATEDB;

CREATE DATABASE entitydb;

GRANT ALL PRIVILEGES ON DATABASE entitydb TO entityadmin;

\du

exit

psql entitydb

CREATE SCHEMA entitysenseschema;

ALTER SCHEMA entitysenseschema OWNER TO entityadmin;

GRANT USAGE, CREATE ON SCHEMA entitysenseschema TO entityadmin;

-- Watchlist Table
CREATE TABLE entitysenseschema.watchlist_entities (
  id SERIAL PRIMARY KEY,
  name TEXT NOT NULL,
  address TEXT,
  country TEXT,
  known_accounts TEXT[],
  risk_category TEXT CHECK (
    risk_category IN (
      'SANCTION', 'MULE', 'MONEY_LAUNDERING',
      'CYBER_THREAT', 'SHELL_ENTITY', 'PEP', 'SCAM_ENTITY'
    )
  ),
  embedding VECTOR(768),
  created_at TIMESTAMP DEFAULT now()
);

-- Vector Index for fast similarity search
CREATE INDEX idx_watchlist_embedding_hnsw
  ON entitysenseschema.watchlist_entities
  USING hnsw (embedding vector_l2_ops)
  WITH (m = 16, ef_construction = 200);
```

---

## How It Works

1. User submits payee info to `/validate-payment`
2. Spring Boot app sends text to Ollama (`nomic-embed-text`) to get a 768-dim vector
3. Vector is compared with existing watchlist vectors in PostgreSQL using `pgvector`
4. Cosine similarity is computed and the response includes:
   - `possibleSanctionEntityMatches`
   - `status`: `BLOCK` or `ALLOW`

---

## Use Cases

- Prevent fund transfers to risky accounts
- Comply with sanctions regulations (OFAC, UN, EU)
- Stop mule-related fraud and laundering networks
- Enhance KYC/AML checks with vector intelligence

---

## Run Locally

```bash
git clone https://github.com/sivabalaji1986/entity-sense.git
cd entity-sense

# Pull embedding model
ollama pull nomic-embed-text

# Start the app
mvn spring-boot:run
```

Access Swagger at: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)

---

# Happy coding!
