<p align="center">
  <img src="../assets/readme/banners/springboot.png" alt="Spring Boot" width="100%">
</p>

# Visão de produto

A pessoa cria uma conta, entra com senha, usa uma identidade federada, confirma o e-mail e recupera a senha. Pode encerrar a sessão atual ou todas as sessões.

A entrada no aplicativo profissional exige autorização pela sessão web, e-mail verificado e elegibilidade profissional. Uma identidade federada com o mesmo e-mail de uma conta local precisa ser vinculada mediante comprovação da senha.

Cinco tentativas de senha incorreta bloqueiam a conta por quinze minutos. A recuperação responde de modo uniforme para não revelar se um endereço está cadastrado. Redefinir ou alterar a senha revoga as sessões existentes.

A decisão de produto permite somente Google e Microsoft no login federado. A validação executada no login ainda está sendo alinhada a essa decisão. A verificação em duas etapas é opcional e ainda não tem rota de configuração. A exclusão de conta também não está disponível.
