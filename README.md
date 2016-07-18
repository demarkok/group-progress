# Прогресс решения задач на информатиксе

На занятиях по программированию я даю ученикам задание решать
задачи на [информатиксе](http://informatics.mccme.ru).
Для того, чтобы отслеживать прогресс решения были написаны эти скрипты.

Основная цель — это создание страниц со статистикой решения задач на разные
темы. В директории example есть [пример](example/progress.html) такой страницы.
Её скриншот можно увидеть ниже.

![Скриншот страницы со статистикой](progress.png)

## Примечания

Данные с информатикса извлекаются путем разбора страниц (как строк).
Это значит, что скрипты могут перестать работать, если разработчики
сайта поменяют формат страниц.

Примеры запуска скриптов в данном документе приведены для Виндоус и выполняются в
корневой директории проекта. Если пример начинается со строки GP-MAIN, то
это сокращение от java -cp "src;target\lib\*" clojure.main -m group-progress.main

Во время написания использовалась Java 8.

Структура страниц с результатами и их внешний вид (включая стили) прописаны в
файле src/group-progress/html.clj.

## Конфигурация

Для сбора статистики необходимо указать идентификаторы учеников и идентификаторы
задач на информатиксе. Пример конфигурационного файла:

```clojure
[{:id "prostokvashino",
  :name "Простоквашино",
  :users
  [{:name "Дядя Фёдор", :mccme 1}
   {:name "Печкин", :mccme 2}],
  :contests
  [{:name "Тема 1",
    :problems
    [{:mccme 3443, :name "Степень двойки."}
     {:mccme 3444, :name "Факториал."}]}
   {:name "Тема 2",
    :problems
    [{:mccme 3501, :name "Максимум двух чисел"}]}]}
 {:id "nez",
  :name "Незнайка и его друзья",
  :accept true,
  :users
  [{:name "Незнайка", :mccme 3}
   {:name "Тюбик", :mccme 4}],
  :contests
  [{:name "Тренировка 1",
    :problems
    [{:mccme 611, :name "Простые числа"}
     {:mccme 612, :name "Выражение"}
     {:mccme 613, :name "Возрастающая подпоследовательность"}]}]}]
```

Описание структуры:

```
Конфигурация
  [первая-группа вторая-группа ...]
Группа
  {:id внутренний-идентификатор-группы
  :name имя-группы,
  :users [первый-участник второй-участник ...],
  :contests [первый-набор-задач второй-набор-задач ...]}
Участник
  {:name имя-участника :mccme идентификатор-на-информатиксе}
Набор задач
  {:name имя-набора
  :problems [первая-задача вторая-задача ...]}
Задача
  {:name название-задачи :mccme идентификатор-на-информатиксе}
```

У группы есть необязательный параметр :accept (см. пример группы
с Незнайкой). Он необходим, чтобы различать решения, которые прошли
все тесты и имеют статус «OK» от решений, которые были просмотрены
учителем и имеют статус «Принято». Если параметр указан, то такие решения
отличаются визуально на итоговой странице.

Для удобства изменения конфигурации, написан скрипт update-config.clj Он
позволяет получить конфигурационный файл из более «простого» файла, в котором
вместо полного описания задачи может быть указан только идентификатор.

```clojure
[
{
  :id "prostokvashino",
  :name "Простоквашино", 
  :users
  [
    {:name "Дядя Фёдор", :mccme 1}
    {:name "Печкин", :mccme 2}
  ],
  :contests
  [
    {:name "Тема 1", :problems [3443 3444]}
    {:name "Тема 2", :problems [3501]}
  ]
}
{
  :id "nez",
  :name "Незнайка и его друзья",
  :accept true,
  :users
  [
    {:name "Незнайка", :mccme 3}
    {:name "Тюбик", :mccme 4}
  ],
  :contests
  [
    {:name "Тренировка 1",:problems [611 {:mccme 612, :name "Выражение"} 613]}
  ]
}
]
```

Запуск скрипта создает файл с дополнительным расширением new в котором
идентификаторы задач заменены на словарь из идентификатора и имени.

```
java -cp "src;target\lib\*" clojure.main update-config.clj example\preconfig.clj
```

## Получение результатов

Для того, чтобы получить страницы с прогрессом необходимо запустить скрипт
group-progress.main, указав ему два обязательных параметра: файл с
настройками и директорию в которой будут созданы страницы. Также можно указать
третий параметр - вспомогательный файл для кеширования.

Сперва имена файлов распознаются как [URI](https://ru.wikipedia.org/wiki/URI),
а затем как локальные файлы. Ниже приведены примеры строк запуска.

```
GP-MAIN config.clj .
GP-MAIN file:./config.clj file:./results/ file:./progress.cache
GP-MAIN https://o8v.github.io/school/groups.clj results/ progress.cache
```

Имена файлов со страницами будут совпадать с внутренними идентификаторами
групп. Для конфигурации из примера файлы будут названы prostokvashino.html и
nez.html.

Что такое файл для кеширования? Для получения результатов скрипт выполняет два
GET запроса для каждого пользователя. Первый позволяет узнать сколько всего
посылок у ученика на информатиксе.  Во втором указывается количество последних
посылок, информацию о которых мы хотим получить. По умолчанию скачиваются
все. Но делать так каждый раз может быть избыточно, поэтому в файле для
кеширования запоминаются уже известные посылки и в следующий раз запрашивается
информация только о новых. При вызове скрипта можно указать несуществующий файл,
тогда скрипт скачает все посылки, создаст файл с указанным именем и сохранит
посылки в этот файл. Этот файл можно периодически удалять. Например,
когда посылки, прошедшие все тесты и получившие «OK» просматриваются учителем и
меняют свой статус.

## Публикация результатов на Амазоне С3

В моем случае, страница с результатами используется учениками и является
заданием, содержащим ссылки на задачи. Поэтому страницы должны быть доступны в
Интернете. При наличии своего сервера можно запускать скрипт так, как описано в
предыдущем пункте и создавать страницы в директории веб-сервера.

Но я добавил возможность взаимодействия с сервисом Амазон С3.
В качестве аргументов скрипт может получать идентификаторы в С3.

Например:

```
GP-MAIN https://o8v.github.io/school/groups.clj s3://e5e
GP-MAIN file:./groups.clj s3://e5e progress.cache
GP-MAIN s3://e5e/groups.clj s3://e5e s3://e5e/progress.cache
```

Здесь подразумевается, что установлен и настроен
[интерфейс командной строки](https://aws.amazon.com/ru/cli/) для
взаимодействия с Амазоном.

Созданные страницы будут открыты для чтения и будут храниться с
[пониженной избыточностью](https://aws.amazon.com/ru/s3/reduced-redundancy/).

В качестве полномочий для доступа к Амазону скрипт использует те, которые
находятся [по умолчанию](http://docs.aws.amazon.com/java-sdk/latest/developer-guide/credentials.html).

## Использование сервиса Амазон Лямбда

Чтобы автоматизировать процесс создания страниц, можно использовать сервис
Амазон Лямбда. Он позволяет определить функцию и указать когда она будет
вызываться.

### Создание роли и настройка прав доступа

В начале необходимо создать роль, с которой будут связаны права доступа,
используемые функцией.

```
aws iam create-role --role-name lambda-progress-role --assume-role-policy-document file://amazon/trustpolicy.json
```

Файл trustpolicy.json:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {"Service": "lambda.amazonaws.com"},
      "Action": "sts:AssumeRole"
    }
  ]
}
```

Добавляю права:

```
aws iam put-role-policy --role-name lambda-progress-role --policy-name lambda-progress-policy --policy-document file://amazon/permissionpolicy.json
```

В файле permissionpolicy.json написано, что функция может вести логи,
читать объекты из С3, помещать объекты в С3, а также указывать права
доступа при создании файлов. Последнее используется, чтобы сделать
страницы доступными для чтения снаружи.

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Action": [
        "logs:CreateLogGroup",
        "logs:CreateLogStream",
        "logs:PutLogEvents"
      ],
      "Resource": "arn:aws:logs:*:*:*",
      "Effect": "Allow"
    },
    {
      "Action": [
        "s3:GetObject",
        "s3:PutObject",
        "s3:PutObjectAcl"
      ],
      "Resource": [
        "arn:aws:s3:::*"
      ],
      "Effect": "Allow"
    }
  ]
}
```

### Подготовка архива для использования в сервисе

С помощью вспомогательного скрипта amazon/build-lambda.clj собирается архив,
который будет заливаться на сервер.

Важно! Конфигурационный файл, имя корзины для результатов и файл для кеширования
при использовании Амазон Лямбда прописываются в теле функции lambda, в файле
src/group-progress/main.clj. Их нужно указать перед сборкой.

```
java -cp "src;target\lib\*" clojure.main .\amazon\build-lambda.clj
```

### Создание функции

```
aws lambda create-function --function-name progress --handler group_progress.main::lambda --runtime java8 --memory 512 --timeout 30 --role arn:aws:iam::671386161117:role/lambda-progress-role --zip-file fileb://./lambda.zip
```

Проверяю, что функция работает, если вызвать ее вручную.

```
aws lambda invoke --function-name progress --log-type Tail outputfile.txt
```

###  Запуск функции по расписанию

Осталось сделать так, чтобы функция вызывалась по расписанию (в примере
ежедневно в 7:30). Время вызова подбирается с учетом разницы между часовыми
поясами.  На сервере он один, а у меня — другой.

```
aws lambda add-permission --function-name progress --statement-id 'Allow-scheduled-events' --action 'lambda:InvokeFunction' --principal 'events.amazonaws.com'
aws events put-rule --name progress-schedule --schedule-expression 'cron(30 07 * * ? *)'
aws events put-targets --rule progress-schedule --targets Id="1",Arn="arn:aws:lambda:eu-west-1:671386161117:function:progress"
```

### Модификация

В любой момент можно пересобрать архив и обновить код на сервере:

```
aws lambda update-function-code --function-name progress --zip-file fileb://./lambda.zip
```

Или изменить настройки:

```
aws lambda update-function-configuration --function-name progress --memory-size 512 --timeout 30
```

### Удаление 

Все делается в обратном порядке.

```
aws events remove-targets --rule progress-schedule --ids 1
aws events delete-rule --name progress-schedule
aws lambda remove-permission --function-name progress --statement-id 'Allow-scheduled-events'
aws lambda delete-function --function-name progress
aws iam delete-role-policy --role-name lambda-progress-role --policy-name lambda-progress-policy
aws iam delete-role --role-name lambda-progress-role
```
