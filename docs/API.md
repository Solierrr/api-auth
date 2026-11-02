<p align="center">
  <img src="../assets/readme/banners/springboot.png" alt="Spring Boot" width="100%">
</p>

# API

O contrato está no [openapi.yaml](https://github.com/Solierrr/api-auth/blob/main/openapi.yaml), com o Swagger em `/docs`. As rotas que emitem tokens aceitam as credenciais próprias de cada fluxo, e as operações autenticadas usam `Authorization: Bearer <access token>`.

| Grupo | Uso |
|---|---|
| `/auth/register`, `/auth/login` | Cadastro e login por senha |
| `/auth/firebase`, `/auth/firebase/link` | Login federado e vínculo com conta local |
| `/auth/refresh`, `/auth/logout` | Renovação e revogação |
| `/auth/email/verification/*`, `/auth/password/*` | Confirmação, recuperação, migração e mudança de senha |
| `/auth/mobile/*` | Transação PKCE, consentimento e troca por sessão mobile |
| `/auth/me`, `/auth/sessions/*` | Perfil da sessão e saídas |
| `/.well-known/jwks.json` | Chave pública |
| `/internal/users/*`, `/internal/sessions/*` | Consulta interna de atividade e sessão |

```http
GET /auth/me
Authorization: Bearer <access token>
```

Os erros seguem [ERRORS.md](https://github.com/Solierrr/docs-warehouse/blob/main/docs/standards/ERRORS.md), e a recuperação de senha pode retornar `INVALID_OR_EXPIRED_TOKEN`, `PASSWORD_POLICY`, `EMAIL_NOT_CONFIGURED` e `EMAIL_UNAVAILABLE`. O contrato descreve os demais códigos, sem que seja preciso repetir aqui cada parâmetro.

As rotas internas são liberadas pela configuração de segurança atual, e o padrão definido exige a `X-Service-Key` de cada chamador nessas rotas, adequação que está pendente.
