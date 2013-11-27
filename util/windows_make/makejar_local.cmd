cd ../../bin
delete ..\build\release\chibipaintmod-devel.jar
jar -mcf ../main-class.txt ../build/release/chibipaintmod-devel.jar chibipaint/

jar -uf ../build/release/chibipaintmod-devel.jar resource/*.*
cd ..

jar -uf ./build/release/chibipaintmod-devel.jar chibipaint-readme.txt
jar -uf ./build/release/chibipaintmod-devel.jar license.txt