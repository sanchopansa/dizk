# Input Data Format Requirements

This project is currently supporting input of *Rank 1 Constraint Systems* (soon to support input of Trusted Setup and Proving Key) for plain-text and JSON file types (eventually binary).

## Rank One Constraint System

### Overview 

- Plain text R1CS defined by 4 files (3 for matrices `a, b` and `c` and 1 for problem size). Each row of matrix files has three space separated integers `c r v` denoting that the non-zero value `v` appears at row `r` and column `c`. Each matrix file **must end with a blank line**!
- JSON R1CS defined as having *constraints* key referring to a list whose rth entry is a triple of key-value pairs denoting the column-values of the rth row of matrices `a, b and c` respectively. See below for examples.
   
### Text

Should consist of 4 files; One for each of matrix `a, b` and `c` and on for the problem size.

**filePath/problem_size** consists of a single line of text with 3 space separated integers, `i a c`, representing numInputs, numAuxiliary and numConstraints respectively.

Each of `filePath/matrix_a, filePath/matrix_b` and `filePath/matrix_c` consists of many rows of space separated integers in the form 
 
 ```
 col row value
 ```
 where `row, col` are the row and column indexes of the (non-zero) entries `value` to be included in the matrix `m` which is determined by the last character of the filename.
 
 **It is important to note** that 
 
 - the rows of these files are assumed to be sorted by the second entry (row),
 - due the the sparse nature of these matrices, only the non-zero entries are included and
 - **files for `a, b` and `c` must end with a blank line!**
 
 Because of the potentially massive file size for these constraint systems, the file is read as an input stream (i.e. **does not** load all of it into memory and then instantiate the Java object)
 
 **For example**: The following three files should be parsed as an input stream and instantiated as a `R1CSRelation` object.
 
 |**r1cs_a** | **r1cs_b**| **r1cs_c** | **problem_size** |
 |-----------|-----------|------------|------------------|
 | 1 0 1     | 0 0 1     | 2 0 1      | i a 3            |
 | 2 0 1     | 3 1 1     | 3 1 1      |                  |
 | 2 1 1     | 1 2 1     | 4 1 2      |                  |
 | 1 2 1     | 2 2 1     |            |                  |
 | 2 2 1     | 3 2 1     |            |                  |
 | 3 2 1     |           |            |                  |
 |           |           |            |                  |
 
 where `i, a` could be any positive integers such that `i + a = 4` (the number of columns)
 
 Would correspond to the matrices
 
 ```
 A = [[0, 1, 1, 0, 0], 
      [0, 0, 1, 0, 0], 
      [0, 1, 1, 1, 0]]
 B = [[1, 0, 0, 0, 0], 
      [0, 0, 0, 1, 0], 
      [0, 1, 1, 1, 0]]
 C = [[0, 0, 1, 0, 0], 
      [0, 0, 0, 1, 0], 
      [0, 0, 0, 0, 2]]
 ```
 
### JSON

The JSON file format of a R1CS (with inputs) consists of 3 keys `primary_inputs, aux_input` and `constraints` each having list values as demonstrated in the following example:

```python
{
  "primary_input": ["1", "0"],
  "aux_input": ["1", "1", "1"],
  "constraints": [
    [{"1": 1, "2": 1}, {"0": 1}, {"2": 1}],
    [{"2": 1}, {"3": 1}, {"3": 1}],
    [{"1": 1, "2": 1, "3": 1}, {"1": 1, "2": 1, "3": 1}, {"4": 1}]
  ]
}
```

where the constraint at index `r` denotes the non-zero entries of the r-th row of matrices `a, b` and `c` respectively. 

- Constraint values can be either of type String or Integer.

In the event that we are only interested in the constraints (without the inputs), this requirements reduces to

```python
{
  "constraints": [
    [{"1": 1, "2": 1}, {"0": 1}, {"2": 1}],
    [{"2": 1}, {"3": 1}, {"3": 1}],
    [{"1": 1, "2": 1, "3": 1}, {"1": 1, "2": 1, "3": 1}, {"4": 1}]
  ]
}
``` 

# Trusted Setup


*TODO*

# Proving Key

*TODO*


