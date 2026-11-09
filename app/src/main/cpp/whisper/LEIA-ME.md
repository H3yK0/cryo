# whisper.cpp (cópia)

Cópia do código-fonte do [whisper.cpp](https://github.com/ggml-org/whisper.cpp) **v1.9.5** (licença MIT, em `LICENSE`), só o
que o Cryo compila: `include/`, `src/` e `ggml/` (núcleo e processador). Compilado pelo `../CMakeLists.txt` (RF-VOZ-10).

Única mudança no código copiado: em `ggml/CMakeLists.txt`, o commit gravado na biblioteca vem do Cryo (`CRYO_GGML_COMMIT`) e
não do `git rev-parse` (esta pasta fica dentro do repositório do Cryo: o git daria o commit do Cryo, com um tamanho que muda de
um clone para outro, e o APK do F-Droid não bateria com o da Release).

Para atualizar: copie as mesmas pastas da versão nova, refaça a mudança acima, compile duas vezes em pastas diferentes e
confira que o APK sai igual.
