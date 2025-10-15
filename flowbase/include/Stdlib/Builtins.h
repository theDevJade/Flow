#ifndef FLOW_BUILTINS_H
#define FLOW_BUILTINS_H

#include <string>

namespace flow {
    namespace stdlib {
        // Note: These allocate memory using 'new' - caller must free!
        int strlen_impl(const char *str);

        const char *substr_impl(const char *str, int start, int len);

        const char *concat_impl(const char *a, const char *b);

        int abs_impl(int x);

        double sqrt_impl(double x);

        double pow_impl(double x, double y);

        int min_impl(int a, int b);

        int max_impl(int a, int b);

        // Note: readLine and readFile allocate memory - caller must free!
        const char *readLine_impl();

        int readInt_impl();

        bool writeFile_impl(const char *path, const char *content);

        const char *readFile_impl(const char *path);
    } // namespace stdlib
} // namespace flow

#endif // FLOW_BUILTINS_H