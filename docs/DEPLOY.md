<p align="center">
  <img src="../assets/readme/banners/springboot.png" alt="Spring Boot" width="100%">
</p>

# Deploy

O workflow de ambiente declara QA em https://api-auth-xw6o.onrender.com e verificação de cluster de produção. Os procedimentos comuns estão em [QA_RENDER.md](https://github.com/Solierrr/docs-warehouse/blob/main/readme/deploy/QA_RENDER.md) e [PROD_GKE_ARGOCD.md](https://github.com/Solierrr/docs-warehouse/blob/main/readme/deploy/PROD_GKE_ARGOCD.md).

Configure por ambiente o banco, o Redis, as chaves JWT, o Firebase e o cliente de e-mail. O readiness usa `/actuator/health/readiness`, com o PostgreSQL como dependência, e o liveness usa `/actuator/health/liveness`.

O cliente transacional de e-mail precisa de URL, chave e endereços de confirmação e recuperação. Esses endereços precisam ser aceitos pelo serviço de envio, que é o caminho definido para todos os e-mails de conta.
