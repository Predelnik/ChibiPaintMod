delete chibipaintmod-devel.jar
cd bin
jar -mcf ../main-class.txt ../chibipaintmod-devel.jar chibipaint/
cd ..

cd bin
jar -uf ../chibipaintmod-devel.jar resource/*.*
cd ..

jar -uf ./chibipaintmod-devel.jar readme.txt
jar -uf ./chibipaintmod-devel.jar license.txt