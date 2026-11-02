<p align="center">
  <img src="../assets/readme/banners/springboot.png" alt="Spring Boot" width="100%">
</p>

# Renovação de sessão

O cliente apresenta o refresh anterior para receber outro e um novo access token. O contexto entre serviços está em [login e tokens](https://github.com/Solierrr/docs-warehouse/blob/main/docs/flows/LOGIN_AND_TOKEN.md) e [provisionamento](https://github.com/Solierrr/docs-warehouse/blob/main/docs/flows/USER_PROVISIONING.md).

```mermaid
flowchart TD
 A[Busca hash do refresh] --> B{Validação aceita?}
 B -->|não| E[Resposta de erro]
 B -->|sim| C[Consome e substitui token]
 C --> D[Entrega novos tokens]
```

Reuso de token consumido revoga a sessão. Sessão revogada ou expirada impede renovação.
