"""
Скрипт для конвертации модели Keras (.keras) в TensorFlow Lite (.tflite)
Запустите этот скрипт перед использованием модели в Android приложении.

Требования:
- tensorflow >= 2.13.0
- Установите: pip install tensorflow

Использование:
python convert_keras_to_tflite.py
"""

import tensorflow as tf
import os

# Путь к исходной модели
INPUT_MODEL_PATH = r"C:\Users\maxim\AndroidStudioProjects\MyApplication\app\src\main\assets\best_resnet101v2.keras"

# Путь для сохранения TFLite модели (папка assets будет создана автоматически)
OUTPUT_DIR = r"C:\Users\maxim\AndroidStudioProjects\ZAAALAA\app\src\main\assets"
OUTPUT_MODEL_PATH = os.path.join(OUTPUT_DIR, "waste_classifier.tflite")

def convert_keras_to_tflite():
    """Конвертирует модель Keras в TensorFlow Lite"""
    
    print("Загрузка модели Keras...")
    
    # Загружаем модель Keras
    try:
        model = tf.keras.models.load_model(INPUT_MODEL_PATH)
        print(f"Модель успешно загружена!")
        print(f"Архитектура модели:")
        model.summary()
    except Exception as e:
        print(f"Ошибка при загрузке модели: {e}")
        return False
    
    # Создаём TFLite конвертер
    print("\nКонвертация в TensorFlow Lite...")
    
    try:
        converter = tf.lite.TFLiteConverter.from_keras_model(model)
        
        # Оптимизация для уменьшения размера (опционально)
        converter.optimizations = [tf.lite.Optimize.DEFAULT]
        
        # Конвертируем
        tflite_model = converter.convert()
        
        # Создаём директорию, если её нет
        os.makedirs(OUTPUT_DIR, exist_ok=True)
        
        # Сохраняем модель
        with open(OUTPUT_MODEL_PATH, 'wb') as f:
            f.write(tflite_model)
        
        file_size = os.path.getsize(OUTPUT_MODEL_PATH)
        print(f"✓ Модель успешно конвертирована!")
        print(f"  Размер файла: {file_size / (1024 * 1024):.2f} MB")
        print(f"  Сохранено в: {OUTPUT_MODEL_PATH}")
        
        return True
        
    except Exception as e:
        print(f"Ошибка при конвертации: {e}")
        return False

if __name__ == "__main__":
    print("=" * 60)
    print("Конвертер Keras -> TensorFlow Lite")
    print("=" * 60)
    
    if not os.path.exists(INPUT_MODEL_PATH):
        print(f"❌ Файл не найден: {INPUT_MODEL_PATH}")
        print("Проверьте путь к исходной модели.")
    else:
        success = convert_keras_to_tflite()
        if success:
            print("\n" + "=" * 60)
            print("✓ Конвертация завершена успешно!")
            print("Теперь можно использовать модель в Android приложении.")
            print("=" * 60)
        else:
            print("\n❌ Конвертация не удалась. Проверьте ошибки выше.")

