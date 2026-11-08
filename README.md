# Raízes do Nordeste

API REST para uma rede de restaurantes. O projeto é a atividade prática da trilha de Back-end do Projeto Multidisciplinar da UNINTER, edição 2026.

A implementação está em andamento e cresce por etapas. Neste momento existem a aplicação Spring Boot, a conexão com o PostgreSQL e o Flyway. Ainda não há entidades, endpoints nem regras de negócio.

## Tecnologias

Estas peças já estão configuradas no projeto:

| Peça | Versão em uso |
|---|---|
| Java | 21, na imagem de execução `eclipse-temurin:21-jre` |
| Spring Boot | 4.1.2 |
| Maven | 3.9, usado somente na imagem de build `maven:3.9-eclipse-temurin-21` |
| PostgreSQL | 16, imagem oficial `postgres:16` |
| Spring Data JPA e Hibernate | versões trazidas pelo Spring Boot 4.1.2 |
| Flyway | versão trazida pelo Spring Boot 4.1.2, com o módulo `flyway-database-postgresql` |
| Driver JDBC do PostgreSQL | versão trazida pelo Spring Boot 4.1.2 |
| Docker e Docker Compose | execução da API e do banco |

## Arquitetura

O código fica no pacote `br.edu.uninter.raizes` e está separado em quatro camadas:

- `api` — reservada para a interface HTTP, com os pacotes `controller`, `dto` e `erro`
- `application` — reservada para os serviços, no pacote `servico`
- `domain` — reservada para o modelo, com os pacotes `entidade`, `enumeracao` e `excecao`
- `infrastructure` — reservada para os detalhes técnicos, com os pacotes `persistencia`, `pagamento`, `seguranca` e `auditoria`

A única classe existente é `RaizesDoNordesteApplication`. Os demais pacotes ainda não têm classes.

## Pré-requisitos

Para executar a aplicação basta ter instalado:

- Docker
- Docker Compose
- Git

Java e Maven não são necessários na máquina local. O build e a execução acontecem dentro dos containers.

## Configuração do ambiente

As credenciais e as portas publicadas ficam no arquivo `.env`, na raiz do projeto. Esse arquivo não é versionado.

```bash
cp .env.example .env
```

No Windows PowerShell:

```powershell
Copy-Item .env.example .env
```

Abra o `.env` e troque a senha de exemplo antes de subir os containers.

| Variável | Quem usa | Finalidade |
|---|---|---|
| `POSTGRES_DB` | PostgreSQL e API | Nome do banco criado na primeira inicialização do volume |
| `POSTGRES_USER` | PostgreSQL e API | Usuário criado nessa mesma inicialização |
| `POSTGRES_PASSWORD` | PostgreSQL e API | Senha desse usuário |
| `POSTGRES_PORT` | Docker Compose | Porta do banco publicada na máquina local |
| `API_PORT` | Docker Compose | Porta da API publicada na máquina local |

O Compose entrega à API estas variáveis, montadas a partir da tabela acima:

- `DB_NAME`, `DB_USER` e `DB_PASSWORD`, com os mesmos valores de `POSTGRES_DB`, `POSTGRES_USER` e `POSTGRES_PASSWORD`
- `DB_PORT=5432`, que é a porta do PostgreSQL dentro da rede Docker, e não a porta publicada na máquina
- `DB_HOST=postgres`, que é o nome do serviço do banco na rede `raizes`

A aplicação monta a URL JDBC com esses valores em `src/main/resources/application.properties`. Os dois containers são independentes e se encontram por essa rede. A API não usa `localhost` para alcançar o banco.

`POSTGRES_DB`, `POSTGRES_USER` e `POSTGRES_PASSWORD` só são aplicados pelo PostgreSQL quando o volume de dados é criado. Um volume que já existe não muda de senha porque o `.env` foi alterado.

Se o arquivo `.env` não existir, o Compose usa os valores padrão escritos em `docker-compose.yml`.

## Executando o projeto

1. Clone o repositório e entre na pasta do projeto.
2. Crie o `.env` a partir do `.env.example`, como na seção anterior.
3. Suba a API e o banco:

```bash
docker compose up --build
```

4. Acompanhe os logs. O PostgreSQL precisa ficar saudável antes de a API iniciar. A aplicação está no ar quando o log mostra `Started RaizesDoNordesteApplication`.
5. Acesse `http://localhost:8080` se `API_PORT` continuar `8080`. Ainda não há endpoints implementados. A raiz responde 404, e isso indica que o servidor subiu.

Outros comandos:

```bash
docker compose logs -f
docker compose down
docker compose up --build
```

`docker compose logs -f` acompanha a saída dos dois serviços. `Ctrl+C` fecha o acompanhamento e não apaga os containers que foram iniciados em segundo plano.

`docker compose down` para e remove os containers e a rede. O volume `raizes-postgres-data` permanece, e os dados do banco continuam disponíveis na próxima subida.

`docker compose down -v` também remove esse volume. Esse comando apaga o banco persistido. Ele não faz parte do uso normal. Use somente quando quiser descartar os dados de propósito, por exemplo para recriar usuário e senha.

`docker compose up --build` reconstrói a imagem da API quando o código ou o `Dockerfile` mudam, e em seguida sobe os dois serviços.

## Banco de dados e migrations

O banco é PostgreSQL 16. O Flyway executa as migrations automaticamente na inicialização da aplicação. Os arquivos SQL ficam em `src/main/resources/db/migration`.

Ainda não há migration de negócio. Na primeira subida, o Flyway cria apenas a tabela de controle `flyway_schema_history` e registra que não havia script a aplicar. O Hibernate está com `ddl-auto=none` e não cria tabelas.

## Documentação da API

O Swagger/OpenAPI ainda não está configurado. Não há URL de documentação neste momento.

## Testes

Não há testes automatizados no projeto. O build da imagem usa `mvn -DskipTests package` porque não existe suíte para executar.

## Estrutura do projeto

```text
raizes-do-nordeste/
├── src/main/java/br/edu/uninter/raizes/
│   ├── RaizesDoNordesteApplication.java
│   ├── api/
│   │   ├── controller/
│   │   ├── dto/
│   │   └── erro/
│   ├── application/
│   │   └── servico/
│   ├── domain/
│   │   ├── entidade/
│   │   ├── enumeracao/
│   │   └── excecao/
│   └── infrastructure/
│       ├── auditoria/
│       ├── pagamento/
│       ├── persistencia/
│       └── seguranca/
├── src/main/resources/
│   ├── application.properties
│   └── db/migration/
├── .dockerignore
├── .env.example
├── .gitignore
├── docker-compose.yml
├── Dockerfile
├── LICENSE
├── pom.xml
└── README.md
```

Os diretórios de `api`, `application`, `domain` e `infrastructure` estão vazios de classes. A pasta `db/migration` ainda não contém scripts SQL.
