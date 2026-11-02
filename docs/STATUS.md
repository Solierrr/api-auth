<p align="center">
  <img src="../assets/readme/banners/springboot.png" alt="Spring Boot" width="100%">
</p>

# Situação

Os itens refletem a main consultada em 2026-10-31.

- Cadastro, login, vínculo federado, rotação de refresh e autorização mobile possuem implementação e testes.
- Recuperação e confirmação já chamam `api-email`, contrariando a indicação antiga de envio por SMTP próprio.
- O cliente de e-mail envia `X-API-Key`, enquanto o padrão atual exige `X-Service-Key`, e a migração para a chave por chamador ainda está pendente.
- TOTP tem estrutura de dados e serviço, mas não tem rota de configuração. A exclusão de conta não tem endpoint implementado.
- A configuração atual libera `/internal/**` sem autenticação por chave, e a adoção da `X-Service-Key` por chamador está pendente.
- A política de Google e Microsoft está registrada no checklist e no schema, mas a validação federada executada ainda não aplica essa restrição.
