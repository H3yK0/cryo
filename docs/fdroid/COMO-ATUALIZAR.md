# Como atualizar o Cryo no F-Droid (guia para fazer sozinho)

Escrito em 23/11/2026, depois do merge request !58632 (a 1.6), que passou nos 9 jobs do CI. A cópia da receita aceita está
em `docs/fdroid/io.github.h3yk0.cryo.yml`.

## 1. A ideia em uma frase

O F-Droid tem um **robô** que olha as etiquetas `v1.7`, `v1.8`… do GitHub. Quando aparece uma nova, ele **copia o último bloco
de `Builds:`** da receita, troca a versão e o commit, compila o Cryo do código-fonte e compara com o `Cryo-X.Y.apk` da Release.
Se os dois forem idênticos, publica. Pense numa receita de bolo: se a receita não muda, o robô faz o bolo novo sozinho.

## 2. Quase sempre: nada a fazer no GitLab

Para uma versão nova **com os mesmos passos de compilação** (mesmo NDK, mesmo CMake, nada novo para instalar):

1. Subir `versionCode` e `versionName` no `app/build.gradle.kts` (o `versionCode` sempre maior que o anterior).
2. Push do commit no GitHub.
3. Criar a Release `vX.Y` (só números depois do `v`) com o arquivo **exatamente** `Cryo-X.Y.apk`.
4. Esperar alguns dias. O robô acrescenta a versão na receita e o servidor publica.

## 3. Quando é preciso abrir merge request

Só quando a **forma de compilar** muda. Exemplos: um NDK novo (`ndkVersion` no `app/build.gradle.kts`), outra versão do CMake,
um programa novo para instalar (`sudo:`), outra pasta do app (`subdir:`), ou um campo da receita (licença, categoria).

### Passo a passo

1. **Atualize o fork antes de tudo.** No seu fork do fdroiddata, use o botão **Update fork** (ou "Sincronizar"). Assim você
   parte da receita **oficial de agora**: o F-Droid pode ter mudado coisas (foi assim com a 1.5, que eles acrescentaram).
2. **Edite o arquivo; não apague e recrie.** Abra `metadata/io.github.h3yk0.cryo.yml` e toque em **Edit**.
3. **Acrescente o bloco novo no fim de `Builds:`**, copiando o último bloco e trocando o que mudou (modelo na seção 4).
4. **Troque o fim do arquivo** para a versão nova (`CurrentVersion` e `CurrentVersionCode`).
5. **Deixe uma linha em branco no fim do arquivo.**
6. Commit (mensagem em inglês: `Update Cryo to X.Y`) e abra o merge request para `fdroid/fdroiddata`, branch `master`.
7. **Confira o pipeline**: os 9 jobs precisam ficar verdes (tabela da seção 5). Se a etiqueta `vX.Y` já existir, a versão
   nova **precisa estar no merge request**, senão o `checkupdates` reprova.

## 4. O modelo do bloco (copie e troque só o que está em ⟨⟩)

```yaml
  - versionName: '⟨1.7⟩'
    versionCode: ⟨12⟩
    commit: ⟨o commit completo da etiqueta v1.7, 40 letras e números⟩
    subdir: app
    sudo: sdkmanager "cmake;3.22.1"
    gradle:
      - yes
    ndk: r27c
```

E o fim do arquivo (estas quatro linhas são obrigatórias, nesta ordem):

```yaml
AutoUpdateMode: Version
UpdateCheckMode: Tags ^v[0-9.]+$
CurrentVersion: '⟨1.7⟩'
CurrentVersionCode: ⟨12⟩
```

O commit completo aparece no GitHub: abra a etiqueta (Releases › `vX.Y` › o número do commit) e copie o código longo.

### As regras de formato (o "corretor" do F-Droid, `fdroid rewritemeta`)

- **Indentação com espaços**, nunca tab: 2 espaços antes do `-` e 4 antes dos campos do bloco.
- **Ordem dos campos dentro do bloco**: `versionName`, `versionCode`, `disable`, `commit`, `subdir`, `sudo`, `gradle`,
  `scanignore`, `ndk`. Campo fora da ordem reprova.
- **Um comando só fica numa linha** (`sudo: sdkmanager "cmake;3.22.1"`); com dois ou mais, vira lista com `-`.
- **Linhas curtas** (até ~100 caracteres); texto longo é quebrado pelo corretor e reprova.
- **Versões entre aspas simples** (`'1.7'`); `versionCode` sem aspas.
- **Linha em branco no fim do arquivo.**
- Para não publicar uma versão: `disable: <motivo curto>`, logo depois do `versionCode`.

## 5. O que cada job do CI quer dizer

| Job | O que confere | Quando reprova, olhe |
|---|---|---|
| Validação de esquema | Os campos obrigatórios e os tipos | Faltou `AutoUpdateMode` ou outra linha do fim |
| fdroid rewritemeta | O formato (seção 4) | Ordem dos campos, lista de um item, linha longa, sem linha no fim |
| fdroid lint | Regras de conteúdo (categoria, links) | O campo que ele citar |
| verificar atualizações (`checkupdates`) | Se o robô ainda acharia versão nova | Falta uma versão que já tem etiqueta, ou o `CurrentVersion` está velho |
| fdroid build | Compila de verdade | O fim do registro do job (o erro aparece nas últimas linhas) |
| verificar apk / código-fonte, redirecionamento git, ferramentas | Conferências gerais | Raramente dão problema |

## 6. Conferir no computador antes (opcional, evita idas e voltas)

Com Python instalado, numa pasta com `metadata/io.github.h3yk0.cryo.yml`:

```bash
pip install fdroidserver check-jsonschema
curl -o metadata.json https://gitlab.com/fdroid/fdroiddata/-/raw/master/schemas/metadata.json
check-jsonschema --schemafile metadata.json metadata/io.github.h3yk0.cryo.yml   # tem que dizer "ok"
git init -q . && git add -A && git commit -qm x
fdroid rewritemeta io.github.h3yk0.cryo && git diff                             # tem que sair vazio
```

Se o `git diff` mostrar algo, o próprio corretor já escreveu o formato certo no arquivo: é só usar o arquivo como ficou.

### Antes de pôr uma biblioteca nova no app (o F-Droid recusa bibliotecas que não são livres)

O servidor do F-Droid roda um **verificador** no código antes de compilar e outro no APK depois. Dá para rodar os dois antes,
com o mesmo `fdroidserver` da seção 6 (foi assim com o Glance do widget, na 1.7):

```bash
fdroid scanner -v -e caminho/do/app-release-unsigned.apk      # o APK: tem que terminar sem "problems" (código de saída 0)
```

E o do código-fonte, que é o que mais importa (ele lê o `gradle/libs.versions.toml` e os `build.gradle.kts`), num script
Python curto que chama `fdroidserver.scanner.scan_source(pasta_do_cryo, build)` com o bloco da última versão; ele tem que
responder 0 problemas. Bibliotecas `androidx.*`, `org.jetbrains.*` e as do Maven Central são livres; o que reprova são
coisas como `com.google.firebase`, `com.google.android.gms` (Play Services), analytics e anúncios — e o Cryo não pode ter
nenhuma delas de qualquer jeito (sem internet). Também vale comparar as permissões do APK novo com as da versão anterior:
`aapt2 dump permissions Cryo-X.Y.apk` (as mesmas, e nunca `INTERNET`).

## 7. Histórico

- **!50813** (06/10/2026): entrada do Cryo no F-Droid. **!56186** (07/11/2026): licença AGPL e a 1.4.
- **!58632** (22–23/11/2026): a 1.6 (NDK r27c e CMake 3.22.1 para a voz). Reprovou primeiro por falta de `AutoUpdateMode` e por
  formato (`disable:` depois de `commit:`, `sudo:` como lista, sem linha no fim) e depois por colar a receita antiga (só até
  a 1.4, `checkupdates`); passou com a receita desta pasta. A 1.5 ficou como os mantenedores a puseram (`scanignore` do modelo).
- **1.7** (23/11/2026): os mesmos passos de compilação da 1.6 (NDK e CMake iguais), então **não precisa de merge request**: o
  robô copia o bloco da 1.6. A biblioteca nova (Glance, do widget) foi conferida com os dois verificadores, sem problema.
