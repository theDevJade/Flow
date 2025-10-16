from setuptools import setup, find_packages

setup(
    name='flow-lang',
    version='0.1.0',
    description='Python bindings for the Flow programming language',
    long_description=open('README.md').read(),
    long_description_content_type='text/markdown',
    author='Flow Language Team',
    url='https://github.com/flow-lang/flow',
    packages=find_packages(),
    python_requires='>=3.7',
    install_requires=[
        # No dependencies - uses stdlib ctypes
    ],
    classifiers=[
        'Development Status :: 3 - Alpha',
        'Intended Audience :: Developers',
        'License :: OSI Approved :: MIT License',
        'Programming Language :: Python :: 3',
        'Programming Language :: Python :: 3.7',
        'Programming Language :: Python :: 3.8',
        'Programming Language :: Python :: 3.9',
        'Programming Language :: Python :: 3.10',
        'Programming Language :: Python :: 3.11',
        'Programming Language :: Python :: 3.12',
        'Topic :: Software Development :: Libraries',
        'Topic :: Software Development :: Compilers',
    ],
    keywords='flow programming-language ffi bindings',
)
