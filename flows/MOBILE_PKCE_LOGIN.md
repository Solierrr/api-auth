<p align="center">
  <img src="../assets/readme/banners/springboot.png" alt="Spring Boot" width="100%">
</p>

# Autorização mobile

O aplicativo inicia uma transação e abre o consentimento na sessão web. O contexto entre serviços está em [login e tokens](https://github.com/Solierrr/docs-warehouse/blob/main/docs/flows/LOGIN_AND_TOKEN.md) e [provisionamento](https://github.com/Solierrr/docs-warehouse/blob/main/docs/flows/USER_PROVISIONING.md).

```mermaid
flowchart TD
 A[Registra desafio PKCE] --> B{Validação aceita?}
 B -->|não| E[Resposta de erro]
 B -->|sim| C[Autoriza com conta elegível]
 C --> D[Troca código e verifier por sessão]
```

São conferidos cliente, redirect, sessão web, validade, consumo único e desafio S256. E-mail não verificado e inelegibilidade bloqueiam a autorização.
