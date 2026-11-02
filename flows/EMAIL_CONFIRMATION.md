<p align="center">
  <img src="../assets/readme/banners/springboot.png" alt="Spring Boot" width="100%">
</p>

# Confirmação de e-mail

A pessoa autenticada pede a mensagem e usa o link para confirmar o endereço. O contexto entre serviços está em [login e tokens](https://github.com/Solierrr/docs-warehouse/blob/main/docs/flows/LOGIN_AND_TOKEN.md) e [provisionamento](https://github.com/Solierrr/docs-warehouse/blob/main/docs/flows/USER_PROVISIONING.md).

```mermaid
flowchart TD
 A[Solicita envio pelo api-email] --> B{Validação aceita?}
 B -->|não| E[Resposta de erro]
 B -->|sim| C[Consome token de confirmação]
 C --> D[Registra e-mail verificado]
```

Token inválido, consumido ou expirado é recusado. O prazo de confirmação é de vinte e quatro horas.
