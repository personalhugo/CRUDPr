# CRUD de Produtos

API REST de CRUD em Java, com Spring Boot e PostgreSQL.

| | |
|---|---|
| Linguagem | Java 21 |
| Framework | Spring Boot 4.1 (Web MVC, Data JPA, Validation) |
| Banco de dados | PostgreSQL 17 |
| Migrações | Flyway |
| Documentação | OpenAPI / Swagger UI (springdoc) |
| Testes | JUnit 5 + MockMvc + Testcontainers (PostgreSQL real) |

## Como executar

Pré-requisitos: **JDK 21+** e **Docker**. O Maven não precisa estar instalado (o projeto traz o Maven Wrapper).

Suba o banco:

```bash
docker compose up -d
```

Inicie a aplicação:

```bash
./mvnw spring-boot:run
```

No Windows, use `mvnw.cmd` no lugar de `./mvnw`.

- API: http://localhost:8080/api/produtos
- Swagger UI: http://localhost:8080/swagger-ui.html

A tabela é criada automaticamente pelo Flyway na primeira subida.

### Usando um PostgreSQL já existente

A conexão é configurada por variáveis de ambiente (os valores padrão são os do `compose.yaml`):

| Variável | Padrão |
|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/produtos` |
| `DB_USERNAME` | `produtos` |
| `DB_PASSWORD` | `produtos` |
| `SERVER_PORT` | `8080` |

```bash
DB_URL=jdbc:postgresql://meu-host:5432/meu_banco DB_USERNAME=usuario DB_PASSWORD=senha ./mvnw spring-boot:run
```

## Endpoints

| Método | Rota | Descrição | Sucesso |
|---|---|---|---|
| `POST` | `/api/produtos` | Cria um produto | `201` + header `Location` |
| `GET` | `/api/produtos` | Lista paginada (filtro opcional por nome) | `200` |
| `GET` | `/api/produtos/{id}` | Busca por id | `200` |
| `PUT` | `/api/produtos/{id}` | Atualiza todos os dados | `200` |
| `DELETE` | `/api/produtos/{id}` | Exclui | `204` |

Parâmetros da listagem: `nome` (trecho do nome), `page` (começa em 0), `size` (padrão 20, máximo 100)
e `sort` (ex.: `sort=preco,desc`; padrão: `nome`).

### Exemplos

```bash
curl -i -X POST http://localhost:8080/api/produtos \
  -H 'Content-Type: application/json' \
  -d '{"sku": "CAM-001", "nome": "Camiseta básica", "descricao": "100% algodão", "preco": 59.90, "quantidadeEstoque": 25}'
```

```bash
curl 'http://localhost:8080/api/produtos?nome=camiseta&page=0&size=10&sort=preco,desc'
```

```bash
curl -X PUT http://localhost:8080/api/produtos/1 \
  -H 'Content-Type: application/json' \
  -d '{"sku": "CAM-001", "nome": "Camiseta premium", "preco": 89.90, "quantidadeEstoque": 10}'
```

```bash
curl -i -X DELETE http://localhost:8080/api/produtos/1
```

### Regras de validação

| Campo | Regra |
|---|---|
| `sku` | obrigatório, 3 a 40 caracteres (letras, números e hífen), único; gravado em maiúsculas |
| `nome` | obrigatório, até 120 caracteres |
| `descricao` | opcional, até 1000 caracteres |
| `preco` | obrigatório, ≥ 0, até 2 casas decimais |
| `quantidadeEstoque` | obrigatório, ≥ 0 |

### Erros

Os erros seguem o formato *Problem Details* (RFC 9457, `application/problem+json`):

| Status | Quando |
|---|---|
| `400` | Dados inválidos, JSON malformado, parâmetro inválido |
| `404` | Produto não encontrado |
| `409` | SKU já em uso, ou edição concorrente do mesmo registro |

```json
{
  "title": "Dados inválidos",
  "status": 400,
  "detail": "Um ou mais campos estão inválidos.",
  "instance": "/api/produtos",
  "erros": [
    { "campo": "nome", "mensagem": "não deve estar em branco" },
    { "campo": "preco", "mensagem": "deve ser maior que ou igual a 0.00" }
  ]
}
```

## Testes

```bash
./mvnw test
```

Os testes são de integração: sobem um PostgreSQL em contêiner (Testcontainers), aplicam as migrações do
Flyway e exercitam a API via HTTP simulado. Por isso, o Docker precisa estar em execução.

## Estrutura

```
src/main/java/com/exemplo/produtos
├── ProdutosApplication.java
├── produto
│   ├── Produto.java              entidade JPA
│   ├── ProdutoRepository.java    acesso a dados (Spring Data)
│   ├── ProdutoService.java       regras de negócio e transações
│   ├── ProdutoController.java    endpoints REST
│   └── dto                       ProdutoRequest / ProdutoResponse
└── comum
    ├── PaginaResponse.java       formato da resposta paginada
    └── erro                      exceções e tratamento global de erros

src/main/resources
├── application.yaml
└── db/migration                  scripts SQL do Flyway (V1__, V2__, ...)
```

Algumas decisões de projeto:

- **O schema pertence ao Flyway**, não ao Hibernate (`ddl-auto: validate`). Para alterar o banco, crie um novo
  script `V2__descricao.sql`; nunca edite um script já aplicado.
- **A API não expõe a entidade JPA**: entrada e saída passam por DTOs (`record`), de modo que mudanças no banco
  não quebram o contrato da API.
- **A unicidade do SKU é garantida pelo banco** (constraint `uk_produto_sku`). A verificação no service existe
  só para devolver uma mensagem amigável.
- **Controle de concorrência otimista** (`@Version`): duas transações simultâneas sobre o mesmo produto não
  sobrescrevem uma à outra silenciosamente; a segunda recebe `409`.
