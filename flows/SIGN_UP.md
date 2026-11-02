<p align="center">
  <img src="../assets/readme/banners/springboot.png" alt="Spring Boot" width="100%">
</p>

# Cadastro

A pessoa informa e-mail e senha. O serviço cria a identidade e grava o evento de provisionamento na mesma transação. O contexto entre serviços está em [login e tokens](https://github.com/Solierrr/docs-warehouse/blob/main/docs/flows/LOGIN_AND_TOKEN.md) e [provisionamento](https://github.com/Solierrr/docs-warehouse/blob/main/docs/flows/USER_PROVISIONING.md).

```mermaid
flowchart TD
 A[Valida cadastro] --> B{Validação aceita?}
 B -->|não| E[Resposta de erro]
 B -->|sim| C[Grava conta e outbox]
 C --> D[Retorna cadastro]
```

E-mail já existente ou entrada inválida impedem o cadastro. O provisionamento do cadastro de negócio é assíncrono.
