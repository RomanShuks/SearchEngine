# SearchEngine

SearchEngine - это поисковый дивжок который позволяет индексировать сайты и выполнять поиск по ключевым словам. 

## Стэк
* Java 17
* Spring
* MySQL 8

## Инструкция
### Заполнить application.yaml 
* Сайтами для индексации<br>
![img_3.png](media/img_3.png)

* подключить локальную БД<br>
* заполнить поля **username** и **password** валидными данными от БД
![img_2.png](media/img_2.png)

### Использование приложения
открыть в браузере http://localhost:8080 
* вкладка DASHBOARD, 
* отображает статусы индексации сайтов<br>
![img_4.png](media/img_4.png)
* вкладка MANAGEMENT
*  START INDEXING - запускает индексацию всех сайтов из application.yaml
*  STOP INDEXING - останавливает индексацию всех сайтов
*  Add/update - запускает добавление, обновление индекса страницы сайта
![img_5.png](media/img_5.png)
* вкладка SEARCH
* осуществляет поиск страниц по переданному поисковому запросу (поле query).
![img_6.png](media/img_6.png)


