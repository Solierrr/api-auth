<p align="center">
  <img src="../assets/readme/banners/springboot.png" alt="Spring Boot" width="100%">
</p>

# Entrada federada

O serviço valida a prova do Firebase e usa a identidade por emissor e sujeito. O contexto entre serviços está em [login e tokens](https://github.com/Solierrr/docs-warehouse/blob/main/docs/flows/LOGIN_AND_TOKEN.md) e [provisionamento](https://github.com/Solierrr/docs-warehouse/blob/main/docs/flows/USER_PROVISIONING.md).

```mermaid
flowchart TD
 A[Valida ID token] --> B{Validação aceita?}
 B -->|não| E[Resposta de erro]
 B -->|sim| C[Localiza ou cria identidade]
 C --> D[Abre sessão]
```

E-mail precisa estar verificado. Uma conta local com o mesmo e-mail exige vínculo e prova da senha.
