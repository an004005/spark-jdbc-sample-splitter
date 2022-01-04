#!/bin/bash

docker run -it -p 55551:55551 \
  -e GRANT_SUDO=yes --user root \
  --name jupyter-notebook \
  -v $(pwd):/home/jovyan/work jupyter/datascience-notebook \
  jupyter notebook --no-browser --allow-root --port 55551 --ip=0.0.0.0 --notebook-dir='~'
