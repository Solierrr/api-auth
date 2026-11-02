<p align="center">
  <img src="../assets/readme/banners/springboot.png" alt="Spring Boot" width="100%">
</p>

# Execução local

Use Java 21, Maven Wrapper, PostgreSQL com `pgcrypto` e `citext`, Redis e Infisical CLI autenticado. O guia comum está em [KOTLIN_SPRINGBOOT.md](https://github.com/Solierrr/docs-warehouse/blob/main/readme/running/KOTLIN_SPRINGBOOT.md).

Execute `make vault` para exportar as pastas `/database`, `/redis`, `/auth`, `/outbox`, `/service-keys`, `/otel`, `/fcm` e `/service-urls`, e depois `make install` e `make dev`, com as chaves JWT e os bancos configurados antes de iniciar.

| Comando | Efeito |
|---|---|
| `make test` | Testes Maven |
| `make lint` | ktlint |
| `make build` | Empacotamento sem testes |
| `make openapi` | Exportação pelo teste `OpenApiExportTests` |

A coleção Bruno está em `http/`. Use `/docs` para consultar a API, `/.well-known/jwks.json` para conferir a chave pública e `/actuator/health/readiness` para verificar o banco. As migrations reais ficam em `src/main/resources/db/migration/`. Variáveis de ambiente podem desligar o Flyway, portanto confirme a origem do schema no ambiente antes de executar migrações.
