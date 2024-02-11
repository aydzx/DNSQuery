package org.example;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

public class Main {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Введите домен");
            return;
        }

        String domain = args[0];
        try {
            // Создание сокета для отправки и получения UDP пакетов
            DatagramSocket socket = new DatagramSocket();

            // Формирование DNS запроса
            byte[] query = createQuery(domain);

            // Отправка DNS запроса на сервер (8.8.8.8)
            InetAddress dnsServer = InetAddress.getByName("8.8.8.8");
            DatagramPacket request = new DatagramPacket(query, query.length, dnsServer, 53);
            socket.send(request);

            // Получение ответа от сервера
            byte[] responseBuffer = new byte[1024];
            DatagramPacket response = new DatagramPacket(responseBuffer, responseBuffer.length);
            socket.receive(response);

            // Разбор ответа и извлечение IP адреса
            String ipAddress = parseResponse(responseBuffer);

            System.out.println("IP адрес для " + domain + ": " + ipAddress);

        } catch (Exception e) {
            System.out.println("Не удалось получить IP адрес для " + domain + ": " + e.getMessage());
        }
}

    private static byte[] createQuery(String domain) {
        byte[] query = new byte[12 + domain.length() + 6];
        // Заполняем заголовок DNS-запроса
        // ID
        query[0] = 0;
        query[1] = 1;
        // Flags
        query[2] = 0;
        query[3] = 0;
        // Questions
        query[4] = 0;
        query[5] = 1;
        // Answer RRs
        query[6] = 0;
        query[7] = 0;
        // Authority RRs
        query[8] = 0;
        query[9] = 0;
        // Additional RRs
        query[10] = 0;
        query[11] = 0;
        // Добавляем домен в запрос
        int position = 12;
        String[] parts = domain.split("\\.");
        for (String part : parts) {
            query[position] = (byte) part.length();
            position++;
            for (char c : part.toCharArray()) {
                query[position] = (byte) c;
                position++;
            }
        }
        query[position] = 0;
        position++;
        // Type (A record)
        query[position] = 0;
        position++;
        query[position] = 1;
        position++;
        // Class (IN)
        query[position] = 0;
        position++;
        query[position] = 1;
        return query;
    }

    private static String parseResponse(byte[] response) {
        // Проверяем, что ответ содержит хотя бы одну запись
        if (response.length < 12) {
            return "Ответ DNS неверного формата";
        }

        // Проверяем, что ответ содержит хотя бы одну запись
        int answerCount = (response[6] & 0xFF) << 8 | (response[7] & 0xFF);
        if (answerCount == 0) {
            return "Для указанного домена нет записи типа A";
        }

        // Ищем начало первой записи в ответе
        int pointer = 12;
        while (response[pointer] != (byte) 0xC0) {
            pointer++;
        }
        pointer += 10; // Пропускаем тип и класс записи

        // Получаем размер записи
        int recordLength = (response[pointer] & 0xFF) << 8 | (response[pointer + 1] & 0xFF);
        pointer += 2; // Пропускаем размер записи

        // Читаем IP адрес из записи
        byte[] ipAddressBytes = new byte[4];
        for (int i = 0; i < 4; i++) {
            ipAddressBytes[i] = response[pointer + i];
        }

        try {
            InetAddress address = InetAddress.getByAddress(ipAddressBytes);
            return address.getHostAddress();
        } catch (UnknownHostException e) {
            return "Не удалось преобразовать IP адрес";
        }
    }
}