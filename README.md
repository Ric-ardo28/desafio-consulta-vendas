# Desafio Consulta Vendas

Implementação do desafio DevSuperior: relatório paginado de vendas e sumário por vendedor, com DTOs e consultas JPQL.

## Executar

Requisito: JDK 21 (ou superior). Spring Boot 4.1.0 e banco H2 em memória, sem banco externo ou configuração de credenciais.

```sh
./mvnw spring-boot:run
```

No Windows, use `mvnw.cmd spring-boot:run`. Na IDE, importe como projeto Maven e execute `DsmetaApplication`.

## Endpoints

- `GET /sales/report`: retorna uma página com `id`, `date`, `amount` e `sellerName`. Aceita `minDate`, `maxDate` e `name` opcionais, além de `page`, `size` e `sort`. A página inicial é 0, o tamanho padrão é 20 e a ordenação padrão é `id,asc`.
- `GET /sales/summary`: retorna uma lista com `sellerName` e `total`, agrupada por vendedor e ordenada pelo nome. Aceita `minDate` e `maxDate` opcionais.
- `GET /sales/{id}`: consulta original preservada.

Datas no formato `yyyy-MM-dd`, com limites inclusivos. Sem `maxDate`, usa a data atual do sistema (`LocalDate.ofInstant(Instant.now(), ZoneId.systemDefault())`). Sem `minDate`, usa um ano antes da data final (`minusYears(1L)`). Parâmetros vazios também usam esses padrões. Sem `name`, considera texto vazio; a pesquisa por trecho do nome ignora maiúsculas e minúsculas.

Exemplos do PDF:

```text
http://localhost:8080/sales/summary?minDate=2022-01-01&maxDate=2022-06-30
http://localhost:8080/sales/report?minDate=2022-05-01&maxDate=2022-05-31&name=odinson
http://localhost:8080/sales/report?minDate=2022-05-01&maxDate=2022-05-31&name=odinson&page=0&size=2&sort=id,asc
```

O sumário do primeiro exemplo retorna Anakin: 110571.0, Logan: 83587.0, Loki Odinson: 150597.0, Padme: 135902.0 e Thor Odinson: 144896.0. O relatório retorna os IDs 9, 10 e 12, com valores 19476.0, 20530.0 e 21753.0.

## Compatibilidade com o enunciado

O repositório original recebeu alterações após o PDF. Para reproduzir exatamente os IDs, datas e valores esperados, `import.sql` foi restaurado do commit inicial `4c58c75`. Com essa base histórica, consultas sem datas retornam listas vazias quando não existem vendas nos últimos 12 meses; o relógio do sistema não é fixado em 2022.

O Spring Boot 4.1.0 do repositório atual foi mantido, e o requisito de compilação foi ajustado de Java 25 para Java 21, usado na validação.

## Testes e leitura do código

```sh
./mvnw clean verify
```

No Windows: `mvnw.cmd clean verify`. Também é possível usar `mvn clean verify` com Maven instalado.

Os testes verificam os resultados exatos do PDF, paginação, ordenação, filtro de nome, limites inclusivos, parâmetros ausentes/vazios e cálculo das datas (incluindo ano bissexto). A collection original do professor está em `postman/desafio-consulta-vendas.postman_collection.json`; importe-a no Postman com a aplicação em execução na porta 8080.

Para estudar a implementação, siga `SaleController` → `SaleService` → `SaleRepository` → `SaleReportDTO` e `SaleSummaryDTO`. O controller recebe strings; o service resolve as datas; o repository executa JPQL com projeções DTO e `SUM`/`GROUP BY`.
