delete chibipaintmod-devel.jar
cd ../../bin
jar -mcf ../main-class.txt ../build/release/chibipaintmod-devel.jar chibipaint/
cd ..

cd bin
jar -uf ../build/release/chibipaintmod-devel.jar resource/*.*
cd ..

jar -uf ./build/release/chibipaintmod-devel.jar chibipaint-readme.txt
jar -uf ./build/release/chibipaintmod-devel.jar license.txt