# Интеграция модели Keras в Android приложение

## Шаги для интеграции модели

### 1. Конвертация модели Keras в TensorFlow Lite

**Важно:** Android приложения используют формат TensorFlow Lite (`.tflite`), а не Keras (`.keras`).

1. Убедитесь, что у вас установлен Python и TensorFlow:
   ```bash
   pip install tensorflow
   ```

2. Запустите скрипт конвертации:
   ```bash
   python convert_keras_to_tflite.py
   ```

3. Скрипт автоматически:
   - Загрузит модель `best_resnet101v2.keras`
   - Конвертирует её в `waste_classifier.tflite`
   - Сохранит в `app/src/main/assets/waste_classifier.tflite`

### 2. Проверка файла модели

После конвертации проверьте, что файл создан:
- `app/src/main/assets/waste_classifier.tflite` должен существовать

### 3. Настройка модели в коде

Файл `WasteClassifier.kt` уже настроен для работы с моделью. Если ваша модель использует другие параметры:

- **Размер входного изображения**: по умолчанию 224x224 (стандарт для ResNet)
- **Нормализация**: (pixel - 127.5) / 127.5
- **Выходные классы**: `["metal", "paper", "glass", "plastic", "other"]`

Если ваша модель использует другие параметры, отредактируйте `WasteClassifier.kt`:
- `INPUT_SIZE` - размер входного изображения
- `IMAGE_MEAN` и `IMAGE_STD` - параметры нормализации
- `OUTPUT_CLASSES` - порядок выходных классов модели

### 4. Использование

После конвертации модели:
1. Синхронизируйте проект: `File` → `Sync Project with Gradle Files`
2. Соберите проект: `Build` → `Rebuild Project`
3. Запустите приложение
4. Откройте экран "Фронтальная камера"
5. Нажмите "Сфотографировать"
6. Модель автоматически классифицирует изображение

### 5. Если модель не работает

Если после конвертации модель не работает:
1. Проверьте, что файл `waste_classifier.tflite` находится в `app/src/main/assets/`
2. Проверьте логи в Logcat на наличие ошибок загрузки модели
3. Убедитесь, что порядок выходных классов совпадает с обученной моделью

### Структура модели

Модель должна:
- Принимать изображение размером 224x224x3 (RGB)
- Возвращать 5 значений (вероятности для каждого класса)
- Классы: `[metal, paper, glass, plastic, other]`

## Альтернативный способ (если скрипт не работает)

Если у вас проблемы с конвертацией, вы можете:

1. Использовать онлайн-конвертер TensorFlow Lite
2. Или использовать TensorFlow в Jupyter Notebook:
   ```python
   import tensorflow as tf
   
   # Загрузите модель
   model = tf.keras.models.load_model("best_resnet101v2.keras")
   
   # Конвертируйте
   converter = tf.lite.TFLiteConverter.from_keras_model(model)
   converter.optimizations = [tf.lite.Optimize.DEFAULT]
   tflite_model = converter.convert()
   
   # Сохраните
   with open("waste_classifier.tflite", "wb") as f:
       f.write(tflite_model)
   ```

