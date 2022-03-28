
cls

del per*.bat
del put\*.class

javac -Xlint -d . source/putClass.java -cp ./lib/*;

echo cls> putClass.bat
echo.>> putClass.bat
echo @java -cp ./lib/*; put.putClass>> putClass.bat
echo.>> putClass.bat
copy putClass.bat + lib\pause.txt putClass.bat



rem @pause>nul
