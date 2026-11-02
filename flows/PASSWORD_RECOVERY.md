<p align="center">
  <img src="../assets/readme/banners/springboot.png" alt="Spring Boot" width="100%">
</p>

# Recuperação de senha

A solicitação de recuperação não revela se uma conta existe. O contexto entre serviços está em [login e tokens](https://github.com/Solierrr/docs-warehouse/blob/main/docs/flows/LOGIN_AND_TOKEN.md) e [provisionamento](https://github.com/Solierrr/docs-warehouse/blob/main/docs/flows/USER_PROVISIONING.md).

```mermaid
flowchart TD
 A[Solicita envio pelo api-email] --> B{Validação aceita?}
 B -->|não| E[Resposta de erro]
 B -->|sim| C[Consome token de uso único]
 C --> D[Altera senha e revoga sessões]
```

O token expira em trinta minutos. Transporte ausente produz EMAIL_NOT_CONFIGURED e falha no envio produz EMAIL_UNAVAILABLE.
