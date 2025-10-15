#include "../../include/Stdlib/Builtins.h"
#include <cstring>
#include <cmath>
#include <iostream>
#include <fstream>
#include <sstream>

namespace flow
{
    namespace stdlib
    {
        int strlen_impl(const char* str)
        {
            return str ? static_cast<int>(std::strlen(str)) : 0;
        }

        const char* substr_impl(const char* str, int start, int len)
        {
            if (!str) return "";

            int strLen = std::strlen(str);
            if (start < 0 || start >= strLen) return "";

            int actualLen = (len > 0 && start + len < strLen) ? len : (strLen - start);
            char* result = new char[actualLen + 1];
            std::strncpy(result, str + start, actualLen);
            result[actualLen] = '\0';
            return result;
        }

        const char* concat_impl(const char* a, const char* b)
        {
            if (!a) a = "";
            if (!b) b = "";

            size_t lenA = std::strlen(a);
            size_t lenB = std::strlen(b);
            char* result = new char[lenA + lenB + 1];

            std::strcpy(result, a);
            std::strcat(result, b);
            return result;
        }


        int abs_impl(int x)
        {
            return std::abs(x);
        }

        double sqrt_impl(double x)
        {
            return std::sqrt(x);
        }

        double pow_impl(double x, double y)
        {
            return std::pow(x, y);
        }

        int min_impl(int a, int b)
        {
            return (a < b) ? a : b;
        }

        int max_impl(int a, int b)
        {
            return (a > b) ? a : b;
        }


        const char* readLine_impl()
        {
            static std::string line;
            if (std::getline(std::cin, line))
            {
                char* result = new char[line.length() + 1];
                std::strcpy(result, line.c_str());
                return result;
            }
            return "";
        }

        int readInt_impl()
        {
            int value = 0;
            std::cin >> value;
            return value;
        }

        bool writeFile_impl(const char* path, const char* content)
        {
            if (!path || !content) return false;

            std::ofstream file(path);
            if (!file.is_open()) return false;

            file << content;
            file.close();
            return true;
        }

        const char* readFile_impl(const char* path)
        {
            if (!path) return "";

            std::ifstream file(path);
            if (!file.is_open()) return "";

            std::stringstream buffer;
            buffer << file.rdbuf();
            file.close();

            std::string content = buffer.str();
            char* result = new char[content.length() + 1];
            std::strcpy(result, content.c_str());
            return result;
        }
    }
}