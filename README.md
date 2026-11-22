# Raízes do Nordeste

API REST da rede de restaurantes Raízes do Nordeste.

Link do repositório: https://github.com/gulinagai/raizes-do-nordeste

O fluxo principal é criar um pedido, registrar um pagamento simulado e atualizar o status até a entrega. A API também reserva e baixa estoque nesse fluxo, permite entrada e saída manuais e guarda o aceite e o saldo de um programa de pontos. Não há cadastro próprio de produto nem consulta de saldo por unidade.

A implementação prioriza o fluxo A, que contempla o processo de criação, pagamento e entrega de pedidos.

## Tecnologias

| Tecnologia | Versão |
|-------------------------------------| --- |
| Java | 21 |
| Spring Boot | 4.1.2 |
| Maven | 3.9, na imagem de build |
| PostgreSQL | 16 |
| Spring Security | Gerenciado pelo projeto |
| JJWT | 0.12.6 |
| Flyway | Gerenciado pelo Spring Boot 4.1.2 |
| springdoc OpenAPI | 3.2.0 |

A aplicação está organizada nas camadas `api`, `application`, `domain` e `infrastructure`.

O Hibernate não cria tabelas (`ddl-auto=none`), o schema vem do Flyway.

## Requisitos

Para subir a API e o banco:

- Docker
- Docker Compose
- Git

Para a suíte automatizada, na máquina local:

- JDK 21
- Maven, disponível no `PATH` como `mvn`

O repositório não tem Maven Wrapper. O build da imagem baixa as dependências do `pom.xml` dentro do container.

## Variáveis de ambiente

Copie o exemplo e ajuste a senha do banco e a chave do JWT antes de subir os containers.

```bash
cp .env.example .env
```

No Windows PowerShell:

```powershell
Copy-Item .env.example .env
```

| Variável | Finalidade |
| --- | --- |
| `POSTGRES_DB` | Nome do banco criado na primeira inicialização do volume |
| `POSTGRES_USER` | Usuário criado nessa inicialização |
| `POSTGRES_PASSWORD` | Senha desse usuário. Troque o valor de exemplo |
| `POSTGRES_PORT` | Porta do PostgreSQL publicada na máquina. O padrão é `5432` |
| `API_PORT` | Porta da API publicada na máquina. O padrão é `8080` |
| `JWT_SECRET` | Chave HMAC-SHA256. Precisa ter pelo menos 32 bytes. Troque o valor de exemplo |
| `JWT_EXPIRATION_SECONDS` | Validade do token, em segundos. O padrão é `3600` |

O arquivo `.env` não é versionado. O Compose repassa o banco para a API como `DB_HOST=postgres`, `DB_PORT=5432`, `DB_NAME`, `DB_USER` e `DB_PASSWORD`. Dentro da rede Docker o host do banco é o serviço `postgres`, não `localhost`.

`POSTGRES_DB`, `POSTGRES_USER` e `POSTGRES_PASSWORD` só valem quando o volume do PostgreSQL é criado. Um volume antigo não muda de senha porque o `.env` mudou.

## Banco, migrations e seed

O PostgreSQL sobe com o Compose. Na inicialização da API, o Flyway aplica os scripts de `src/main/resources/db/migration`, da V1 à V12.

O seed está nessas migrations. A V2 cria cinco usuários de demonstração. A V4 cria a matriz, a filial, três produtos e o estoque inicial. O Baião de dois, na matriz, começa com 40 unidades a R$ 34,90.

## Subir a API

Na raiz do projeto, com o `.env` já ajustado:

```bash
docker compose up --build -d
```

O banco precisa ficar saudável antes da API. A aplicação está no ar quando o log mostra `Started RaizesDoNordesteApplication`.

```bash
docker compose logs -f api
```

Com `API_PORT=8080`, a base é `http://localhost:8080`.

`docker compose down` para os containers e mantém o volume `raizes-postgres-data`. `docker compose down -v` apaga esse volume e, com ele, o banco. Use só quando quiser recriar os dados do zero. O Flyway não reaplica o seed em um banco que já passou pelas migrations.

## Documentação da API

Com a API no ar:

- Swagger UI: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- OpenAPI: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

As duas rotas são públicas. No Swagger, informe só o `accessToken` obtido no login, a interface acrescenta o prefixo `Bearer`.

## Autenticação

O login é `POST /auth/login` e devolve um JWT. Cadastro e login são públicos. As outras rotas da API exigem o token.

Os usuários de demonstração usam a senha `Raizes@2026`:

| E-mail | Perfil |
| --- | --- |
| `admin@raizes.test` | ADMIN |
| `gerente@raizes.test` | GERENTE |
| `atendente@raizes.test` | ATENDENTE |
| `cozinha@raizes.test` | COZINHA |
| `cliente@raizes.test` | CLIENTE |

O cliente opera os próprios pedidos e a fidelidade. A cozinha avança o pedido para `PRONTO`. O atendente avança para `ENTREGUE`. Estoque manual fica com ADMIN e GERENTE.

## Testes

### Suíte automatizada

Suba só o PostgreSQL e exporte as mesmas credenciais do `.env`, porque os testes usam o banco publicado em `localhost`. O perfil `test` define o JWT da suíte. As variáveis de conexão, se não forem informadas, usam os padrões `localhost:5432`, banco `raizes`, usuário `raizes` e senha `raizes`.

```powershell
docker compose up -d postgres
$env:DB_HOST = "localhost"
$env:DB_PORT = "5432"
$env:DB_NAME = "raizes"
$env:DB_USER = "raizes_usuario"
$env:DB_PASSWORD = "altere-esta-senha"
mvn clean verify
```

Troque `DB_PORT`, `DB_NAME`, `DB_USER` e `DB_PASSWORD` pelos valores do seu `.env`. `DB_USER` e `DB_PASSWORD` precisam ser os de `POSTGRES_USER` e `POSTGRES_PASSWORD`. O exemplo acima utiliza os valores de conexão definidos no `.env.example`, considerando que eles correspondam aos valores informados no comando.

A suíte sobe a aplicação de teste, aplica o Flyway se ainda houver migration pendente e desfaz os dados de cada teste ao final. A API em Docker não precisa estar no ar.

### Coleção Postman

A coleção e o plano estão em:

- `docs/entrega-final/postman/raizes-do-nordeste.postman_collection.json`
- `docs/entrega-final/postman/plano-de-testes.md`

Importe a coleção no Postman e execute de cima para baixo, com a API no ar. Os logins gravam os tokens. A ordem, as pré-condições e o efeito no estoque estão no plano. Cada execução completa do fluxo baixa uma unidade do Baião de dois na matriz.

Privacidade, segurança e o que a API não faz em relação à LGPD estão em `docs/entrega-final/privacidade-seguranca-lgpd.md`.

## O que a API oferece

- `POST /usuarios` e `POST /auth/login`, além de `GET /auth/me`
- `GET /unidades/{id}/cardapio`
- `POST /pedidos`, `GET /pedidos`, `PATCH /pedidos/{id}/status` e `PATCH /pedidos/{id}/cancelamento`
- `POST /pagamentos`, com resultado simulado
- `POST /estoque/movimentacoes`
- `GET /fidelidade`, `PUT /fidelidade` e `POST /fidelidade/resgates`

As falhas usam o mesmo JSON, com `error`, `message`, `details`, `timestamp`, `path` e `requestId`. Validação responde 422.