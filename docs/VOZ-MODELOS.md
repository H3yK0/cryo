# Modelos de voz do Cryo (RF-VOZ-15)

Desde a 1.6 o modelo do Whisper **não vem mais dentro do app**. O F-Droid compila o Cryo do código-fonte e o scanner dele
recusa arquivos binários prontos no repositório; o modelo são 57 MB de pesos, que não saem de código nenhum (ERR-057). O
Cryo continua **sem permissão de internet**: quem baixa é o navegador.

## Como funciona para quem usa

1. Na primeira vez que toca no microfone (ou em Ajustes › Voz › Modelo de voz), o Cryo explica e mostra
   o botão **Baixar o modelo**. Ele abre o navegador no arquivo recomendado.
2. Depois de baixar, **Já baixei: escolher o arquivo** abre o seletor de arquivos do Android.
3. O Cryo copia o arquivo para a área privada dele (`noBackupFilesDir/whisper/`), calculando o **SHA-256** no caminho. Só
   entra um arquivo cujo tamanho e SHA-256 batem com um dos modelos da tabela abaixo. Qualquer outro é recusado e apagado.
4. O arquivo baixado pode ser apagado da pasta Downloads depois: o Cryo guarda a cópia dele.

Um modelo por vez: escolher outro troca (o de antes só sai depois que o novo foi conferido). O modelo fica fora do backup
(é do aparelho, não da pessoa, e dá para baixar de novo). Se o arquivo guardado mudar depois (limpeza do sistema, outro
app), o Cryo percebe antes de usar, tira o modelo e avisa ("deixou de bater com o original"). A cada uso ele confere o tamanho e a data; no **primeiro uso de cada abertura do app**, confere também o **SHA-256 inteiro** (pega um arquivo mexido que manteve o tamanho e a data; com o modelo recomendado leva uns décimos de segundo, uma vez, enquanto o microfone liga). Um modelo que bate com o SHA-256 e mesmo assim não abre (falta de memória, por exemplo) **não é apagado**: desta vez a voz usa o reconhecedor do celular. Se o Android fechar o app no meio da cópia, o arquivo temporário é apagado na próxima vez que o Cryo olhar o modelo (ERR-066).

Sem modelo, a voz usa o reconhecedor do próprio celular (só no aparelho, quando ele tem) ou a tela de voz do sistema.

## Modelos aceitos

Todos são do projeto [whisper.cpp](https://huggingface.co/ggerganov/whisper.cpp) (formato `ggml`, multilíngues).

| Modelo | Arquivo | Tamanho (bytes) | SHA-256 |
|---|---|---|---|
| Tiny (5 bits) | `ggml-tiny-q5_1.bin` | 32.152.673 | `818710568da3ca15689e31a743197b520007872ff9576237bda97bd1b469c3d7` |
| Tiny (8 bits) | `ggml-tiny-q8_0.bin` | 43.537.433 | `c2085835d3f50733e2ff6e4b41ae8a2b8d8110461e18821b09a15c40c42d1cca` |
| **Base (5 bits)** · recomendado | `ggml-base-q5_1.bin` | 59.707.625 | `422f1ae452ade6f30a004d7e5c6a43195e4433bc370bf23fac9cc591f01a8898` |
| Base (8 bits) | `ggml-base-q8_0.bin` | 81.768.585 | `c577b9a86e7e048a0b7eada054f4dd79a56bbfa911fbdacf900ac5b567cbb7d9` |
| Small (5 bits) | `ggml-small-q5_1.bin` | 190.085.487 | `ae85e4a935d7a567bd102fe55afc16bb595bdb618e11b2fc7591bc08120411bb` |
| Small (8 bits) | `ggml-small-q8_0.bin` | 264.464.607 | `49c8fb02b65e6049d5fa6c04f81f53b867b5ec9540406812c643f177317f779f` |

O recomendado é o **mesmo arquivo** que ia dentro do Cryo até a 1.5 (mesmo SHA-256). A lista fica em
`app/src/main/java/io/github/h3yk0/cryo/voice/WhisperModels.kt`; mudar um número aqui sem mudar lá (ou o contrário) faz o
`WhisperModelStoreTest` falhar.

## De onde baixar

- **Origem oficial** (todos): `https://huggingface.co/ggerganov/whisper.cpp/resolve/main/<arquivo>`
- **GitHub do Cryo** (só o recomendado): a Release **`voice-model-1`**,
  `https://github.com/H3yK0/cryo/releases/download/voice-model-1/ggml-base-q5_1.bin`.

A Release `voice-model-1` é fixa (não muda a cada versão do app) e é criada com **Target = branch `versao-1.4`**: assim a
etiqueta fica num código antigo e o robô do F-Droid, que só olha etiquetas `v*` (`UpdateCheckMode: Tags ^v[0-9.]+$`), não a
toma por uma versão. Ela leva o arquivo `ggml-base-q5_1.bin` e o texto da licença abaixo.

## Para quem desenvolve

- `voice/WhisperModels.kt`: a tabela acima. `voice/WhisperModelStore.kt`: importar (`install`), conferir de novo antes de
  usar (`usableFile`: tamanho e data a cada uso, SHA-256 no primeiro uso de cada abertura), abrir sem apagar um modelo bom
  que não abriu (`openWith`), remover. Resultados: `Installed`, `NoSpace` (com quanto liberar: o modelo inteiro mais 50 MB de
  folga), `Incomplete` (começa como modelo, mas o SHA-256 não bate), `NotAModel`, `Unreadable`.
- `ui/screens/VoiceModelUi.kt`: a folha "Baixe a voz do Cryo uma vez" e a seção dos Ajustes.
- A ponte nativa abre o arquivo (`WhisperNative.nativeOpenFile`); a biblioteca `android` saiu do link (não há mais assets).
- O teste no computador (`app/src/test/cpp/build-host.sh`) baixa o `ggml-base-q5_1.bin` da origem oficial para
  `app/build/whisper-host/` e confere o SHA-256 antes de compilar (ou use `CRYO_WHISPER_MODEL=/caminho/do/arquivo`).
- `SourceTreeTest` garante que nenhum binário (modelo, biblioteca, pacote) volte para o repositório.

## Licença dos modelos

Os modelos Whisper são da OpenAI, sob a licença MIT (a mesma do código do Whisper e do whisper.cpp):

```
MIT License

Copyright (c) 2022 OpenAI

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```
