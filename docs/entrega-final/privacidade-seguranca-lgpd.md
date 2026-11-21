# Privacidade, segurança e LGPD

Este documento apresenta como a API Raízes do Nordeste trabalha com dados pessoais e quais medidas de segurança foram implementadas. O objetivo é registrar o funcionamento atual da aplicação, incluindo os cuidados adotados e os pontos que ainda podem ser melhorados.

  Dados pessoais

A aplicação armazena informações relacionadas às contas dos usuários, aos pedidos e ao programa de fidelidade. Não são coletados CPF, telefone, endereço residencial ou dados de cartão. O endereço cadastrado no sistema pertence à unidade do restaurante, não ao cliente.

A tabela abaixo apresenta os principais dados armazenados, sua finalidade e como aparecem nas respostas da API.

| Dado | Onde fica | Para que serve | O que a API devolve |
| --- | --- | --- | --- |
| Nome | `usuario.nome` | Identificar o usuário na aplicação | Id, nome e perfil em `UsuarioResposta` |
| E-mail | `usuario.email` | Identificação no login e controle de contas duplicadas | Não é devolvido |
| Senha | `usuario.senha_hash` | Autenticação do usuário | Apenas o hash é armazenado |
| Perfil e situação da conta | `usuario.perfil`, `usuario.ativo` | Controlar permissões e acesso | O perfil é devolvido, mas a situação da conta não |
| Pedido e pagamento | `pedido`, `pagamento` | Registrar e acompanhar as compras | Id do cliente, itens, valores e status, sem e-mail |
| Fidelidade | `fidelidade` | Registrar o aceite e controlar os pontos | Aceite, datas e saldo do próprio cliente |

Durante o cadastro, o e-mail é convertido para letras minúsculas e tem os espaços das extremidades removidos. Além disso, o cadastro público permite criar somente usuários com perfil `CLIENTE`, evitando que alguém obtenha permissões administrativas diretamente pelo endpoint de registro.

  ## Finalidade do tratamento e relação com a LGPD

Os dados utilizados no cadastro e na autenticação são necessários para identificar o usuário e permitir que ele utilize os serviços da aplicação. No contexto do projeto, esse tratamento está relacionado à execução do serviço solicitado pelo próprio usuário.
As informações de fidelidade são utilizadas exclusivamente para o funcionamento do programa de pontos. O aceite desse programa é registrado separadamente e está disponível apenas para usuários com perfil `CLIENTE`. Portanto, aceitar participar da fidelidade não é uma condição para criar uma conta ou utilizar os demais recursos da API.

A auditoria também utiliza informações relacionadas aos usuários, mas com uma finalidade diferente: identificar quem realizou determinadas operações e manter um histórico das ações executadas.

Essas finalidades foram definidas considerando o funcionamento implementado no sistema.

  ## Aceite do programa de fidelidade

O endpoint `PUT /fidelidade` permite que o cliente autenticado aceite ou revogue sua participação no programa de pontos.
A tabela `fidelidade` mantém o estado atual do aceite, a data em que ele foi registrado e, quando aplicável, a data da revogação. O saldo de pontos começa em zero.
Quando o cliente aceita participar, a aplicação permite que ele acumule pontos por meio das entregas e utilize esses pontos para obter descontos nos próprios pedidos. Caso o aceite seja revogado, novas operações de crédito e resgate ficam bloqueadas.
A revogação não apaga o registro nem zera o saldo acumulado. Caso o cliente aceite participar novamente, o programa é reativado mantendo os pontos que já estavam registrados.

Também existe um tratamento específico para devoluções de pontos. Quando um resgate anterior precisa ser desfeito, a devolução não depende de o aceite estar ativo naquele momento.
Dessa forma, a aplicação mantém o histórico das alterações realizadas pelo cliente e permite identificar quando o aceite foi concedido ou revogado. Esse registro representa a informação fornecida pelo usuário à API.

  ## Medidas de segurança

Alguns cuidados foram implementados para proteger as informações armazenadas e controlar o acesso aos recursos da aplicação.

As senhas são processadas com BCrypt antes de serem armazenadas no banco de dados. O cadastro também valida o tamanho da senha, permitindo valores entre 8 e 72 caracteres. A senha original não é persistida.

No login, a aplicação utiliza a mesma resposta (`CREDENCIAIS_INVALIDAS`) para situações de e-mail inexistente, senha incorreta ou conta inativa. Quando o e-mail não é encontrado, também é realizada uma comparação utilizando um hash fixo. Essa medida busca reduzir diferenças no tempo de resposta que poderiam indicar se determinada conta existe.

A autenticação utiliza JWT com algoritmo HS256. A chave de assinatura é obtida pela variável de ambiente `JWT_SECRET`, que deve possuir pelo menos 32 bytes. O tempo de validade do token é configurado por `JWT_EXPIRATION_SECONDS`.

Essas configurações ficam fora do código-fonte, no ambiente de execução. O token contém o identificador do usuário, seu perfil e as informações de emissão e expiração. Dados como nome, e-mail e senha não são incluídos.

A aplicação utiliza autenticação stateless, sem armazenamento de sessão ou utilização de cookies para manter o usuário autenticado.

A cada requisição, o contexto de segurança é limpo antes da execução do filtro de autenticação. Quando um token é recebido, seu identificador é utilizado para consultar o usuário no banco. A autenticação só é concluída se a conta estiver ativa. As permissões são definidas com base no perfil encontrado no banco de dados, e não apenas na informação presente no token.

As respostas relacionadas aos usuários também possuem restrições. O e-mail, o hash da senha e a situação da conta não são expostos nos DTOs de resposta.

Em caso de erro inesperado, a API retorna `ERRO_INTERNO` com uma mensagem genérica. Os detalhes técnicos permanecem nos registros do servidor, acompanhados da URI da requisição, para auxiliar na investigação de problemas.

Um ponto importante é que a aplicação não configura TLS diretamente. Caso seja publicada em um ambiente que utilize HTTPS por meio de um proxy reverso, a proteção da comunicação ficará sob responsabilidade dessa infraestrutura.

  ## Autenticação e controle de acesso

As rotas `POST /usuarios` e `POST /auth/login` são públicas, pois permitem respectivamente o cadastro e a autenticação dos usuários. A documentação OpenAPI também pode ser acessada sem token.

As demais operações exigem autenticação. Além disso, os perfis possuem permissões diferentes, de acordo com as responsabilidades de cada tipo de usuário.

| Operação | Perfis autorizados |
| --- | --- |
| Movimentar estoque | `ADMIN`, `GERENTE` |
| Criar pedido, cancelar e pagar | `ADMIN`, `GERENTE`, `ATENDENTE`, `CLIENTE` |
| Avançar status do pedido | `ADMIN`, `GERENTE`, `COZINHA`, `ATENDENTE` |
| Consultar cardápio, listar pedidos e consultar o próprio usuário | Qualquer perfil autenticado |
| Utilizar a fidelidade | `CLIENTE` |

Além das permissões por perfil, existem regras específicas para evitar que um usuário acesse operações de outros clientes.

O cliente pode criar, cancelar, pagar e utilizar pontos somente nos próprios pedidos. Na consulta de pedidos, a API também restringe os resultados aos registros associados ao usuário autenticado.

Os perfis operacionais possuem restrições adicionais no fluxo dos pedidos. O usuário com perfil `COZINHA` pode avançar o pedido para `PRONTO`, enquanto o perfil `ATENDENTE` pode solicitar a mudança para `ENTREGUE`.

Essas regras são verificadas pela aplicação durante a execução das operações, complementando o controle de acesso definido na configuração de segurança.

  ## Auditoria

A aplicação possui um mecanismo de auditoria que registra determinadas operações realizadas pelos usuários.

Cada registro é armazenado na tabela `registro_auditoria` e contém o identificador do usuário responsável, a ação executada, o tipo de recurso, o identificador desse recurso, um resultado resumido e a data da operação.

Para evitar armazenar informações desnecessárias, o registro não inclui senhas, tokens, e-mails, nomes, corpos das requisições ou valores financeiros.

Entre as operações monitoradas estão:

- Cadastro e autenticação de usuários;
- Consulta do próprio usuário;
- Consulta de pedidos;
- Criação e cancelamento de pedidos;
- Atualização do status;
- Processamento de pagamentos;
- Movimentações de estoque;
- Registro e alteração do aceite da fidelidade;
- Crédito, resgate e devolução de pontos.

Como o cadastro é uma operação pública, seu registro de auditoria não possui um usuário autenticado associado.

No caso do login, somente as autenticações realizadas com sucesso são registradas. As tentativas que resultam em falha não geram registros de auditoria.

A consulta de pedidos também é registrada, mas a lista de resultados não é armazenada na auditoria.

Atualmente, não existe um endpoint para consultar esses registros pela API. A verificação automatizada desse comportamento é realizada por meio do teste `AuditoriaTest`, enquanto a conferência diretamente no banco pode ser feita consultando a tabela `registro_auditoria`.

  ## Integrações

O processamento de pagamentos é simulado dentro da própria aplicação, por meio da classe `SimuladorDePagamento`.

Nesse fluxo, o cliente informa o resultado que deseja simular, e o sistema retorna uma das situações previstas: pagamento aprovado, recusado ou indisponível.

Como não existe integração com um serviço externo de pagamentos, nenhum dado é encaminhado a uma instituição financeira. A API também não solicita número de cartão nem realiza o envio de e-mails.

  ## Direitos do titular

O cliente autenticado possui acesso a algumas informações relacionadas à sua conta por meio dos endpoints disponibilizados.

Atualmente, é possível:

- Consultar o próprio perfil em `GET /auth/me`;
- Consultar os próprios pedidos em `GET /pedidos`;
- Consultar as informações de fidelidade em `GET /fidelidade`;
- Registrar ou revogar o aceite do programa de pontos.

Essas operações permitem que o usuário acompanhe parte das informações mantidas pela aplicação.

Por outro lado, ainda não existem funcionalidades para corrigir os dados cadastrais, exportar todas as informações pessoais, excluir uma conta ou anonimizar o histórico de pedidos e pagamentos.

Esses recursos representam pontos que podem ser desenvolvidos em versões futuras, considerando também os direitos previstos na LGPD.

  ## Retenção dos dados

Atualmente, a aplicação não possui uma política automatizada de retenção ou uma rotina de anonimização dos dados.
As informações de usuários, pedidos, pagamentos, fidelidade e auditoria permanecem armazenadas no PostgreSQL até que sejam removidas diretamente do banco de dados.
As migrations utilizadas pelo projeto não realizam a exclusão desses registros durante uma nova inicialização da aplicação. Portanto, reiniciar o sistema ou executar novamente o processo de inicialização não significa que os dados existentes serão apagados.
A definição de prazos de retenção e dos procedimentos de exclusão ainda é uma melhoria pendente.

