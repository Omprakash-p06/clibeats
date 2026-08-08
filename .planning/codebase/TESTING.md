# Codebase Testing Strategy: CliBeats

## 1. Android Application Testing
- **Unit Testing**:
  - JUnit 4 + Google Truth assertions.
  - Mockito Kotlin (`mockito-kotlin` 5.4.0) for mocking repository and network dependencies.
  - Coroutines testing (`kotlinx-coroutines-test`).
- **Database & DAO Integration Tests**:
  - In-memory database builders (`Room.inMemoryDatabaseBuilder`) executed under `AndroidJUnit4` runner.
  - Verifies Room migrations, index constraints, foreign keys, and DAO queries.
- **UI Screenshot / Component Testing**:
  - Paparazzi (1.3.4) for rendering and snapshot testing of Jetpack Compose components (`PaparazziDebugResources`).
- **Run Command**:
  ```bash
  .\gradlew.bat testDebugUnitTest
  ```

---

## 2. Gateway Testing (Node.js / TypeScript)
- **Unit & Integration Testing**:
  - Vitest 3.0.4 test runner (`npm test` in `gateway/`).
  - Unit tests for `YouTubeProviderAdapter`, cache keys, error schema mappers.
- **Contract & OpenAPI Validation**:
  - `npm run openapi:generate` & `npm run openapi:validate`.
- **Property-based & Load Testing**:
  - Fast Check (`fast-check`) for property testing.
  - Autocannon (`autocannon`) for load testing (`npm run test:load`).
- **Run Command**:
  ```bash
  cd gateway && npm test
  ```
