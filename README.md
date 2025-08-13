# SQL-on-CSV (`sqlcsv`)

`sqlcsv` é uma ferramenta de linha de comando (CLI) escrita em Java 21 que permite executar uma sub-linguagem SQL sobre arquivos CSV. O projeto foi construído do zero, incluindo um lexer e um parser Pratt customizados, sem dependências externas para a análise da query.

## Funcionalidades

* **Query Engine customizado**: Parser e executor implementados manualmente.
* **Suporte a SQL Básico**: `SELECT`, `FROM`, `WHERE`, `ORDER BY`, `LIMIT`.
* **Expressões**: Suporte a expressões aritméticas, lógicas e de comparação com precedência de operadores correta.
* **Aliases de Coluna**: `SELECT col * 2 AS novo_nome ...`
* **Streaming**: O filtro `WHERE` é aplicado linha a linha para economizar memória.
* **CLI amigável**: Entrada e saída via linha de comando.

## Build

O projeto usa Maven. Para construir um JAR executável:

```bash
mvn clean package
```

Isso irá gerar `target/sqlcsv-1.0.0.jar`.

## Como Usar

O binário `sqlcsv` aceita uma query SQL e o caminho para um arquivo CSV.

### Sintaxe

```bash
java -jar target/sqlcsv-1.0.0.jar "SUA QUERY SQL" --csv /caminho/para/o/arquivo.csv [OPÇÕES]
```

### Argumentos

* **Query String**: A query SQL a ser executada, entre aspas.
* `--csv <path>`: (Obrigatório) O caminho para o arquivo CSV a ser lido. O nome do arquivo (sem extensão) é usado como nome da tabela.
* `--header <true|false>`: (Opcional) Indica se a primeira linha do CSV é o cabeçalho. Padrão: `true`.
* `--separator <char>`: (Opcional) Caractere separador de colunas. Padrão: `,`.
* `--null-str <string>`: (Opcional) String que deve ser interpretada como `NULL`. Padrão: `""` (string vazia).

### Exemplo de Uso

Dado o arquivo `samples/data/vendas.csv`:

```csv
id,data,quantidade,preco,cancelado
1,2024-01-10,2,10.5,false
2,2024-01-11,1,8.0,true
3,2024-02-01,5,7.2,false
4,2024-02-05,3,15.0,false
```

**Query:** Selecionar vendas não canceladas, calcular o total e ordenar pelo maior total.

```bash
java -jar target/sqlcsv-1.0.0.jar \
  "SELECT id, quantidade * preco AS total FROM vendas WHERE NOT cancelado AND preco >= 7.2 ORDER BY total DESC LIMIT 2" \
  --csv samples/data/vendas.csv
```

**Saída Esperada:**

```
+----+-------+
| id | total |
+----+-------+
| 4  | 45.0  |
| 3  | 36.0  |
+----+-------+
Rows: 2
```

## Gramática Suportada (EBNF)

```
selectStmt   := SELECT projList FROM tableRef whereClause? orderBy? limit?;
projList     := ("*" | expr (AS? ident)?)(, expr (AS? ident)?)*;
tableRef     := ident;
whereClause  := WHERE expr;
orderBy      := ORDER BY orderTerm (',' orderTerm)*;
orderTerm    := expr (ASC|DESC)?;
limit        := LIMIT INT;
expr         := logicOr;
logicOr      := logicAnd (OR logicAnd)*;
logicAnd     := equality (AND equality)*;
equality     := comparison ((= | <> | !=) comparison)*;
comparison   := additive ((< | <= | > | >=) additive)*;
additive     := multiplicative ((+ | -) multiplicative)*;
multiplicative := unary ((* | / | %) unary)*;
unary        := (NOT | + | -) unary | primary;
primary      := literal | ident | '(' expr ')';
literal      := INT | DOUBLE | STRING | TRUE | FALSE | NULL;
```

## Limitações e Roadmap

Esta é uma primeira versão com um escopo deliberadamente limitado.

### Limitações Atuais

* **ORDER BY em memória**: A ordenação carrega todos os resultados filtrados na memória. Não é adequado para datasets gigantescos.
* **Sem Joins**: Apenas uma tabela (um arquivo CSV) por query.
* **Sem agregações**: `GROUP BY`, `COUNT`, `SUM`, `AVG` não são suportados.
* **Tipos de dados simples**: Apenas `STRING`, `DOUBLE`, `BOOLEAN` e `NULL`. Não há `DATE` ou `TIMESTAMP`.

### Roadmap Futuro

* [ ] **External Sort**: Implementar ordenação em disco para `ORDER BY` em arquivos grandes.
* [ ] **Agregações**: Adicionar suporte a `GROUP BY` e funções de agregação.
* [ ] **JOINs**: Implementar `INNER` e `LEFT JOIN` com outros arquivos CSV.
* [ ] **Funções de String/Data**: Adicionar funções como `LOWER()`, `SUBSTRING()`, `DATE_TRUNC()`.
* [ ] **Melhorias no Formato de Saída**: Adicionar opção para salvar o resultado em um novo CSV (`--out out.csv`).