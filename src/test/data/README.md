# Input Data Format Requirements

This project is currently supporting input of *Rank 1 Constraint Systems* (soon to support input of Trusted Setup and Proving Key) for plain-text and JSON file types (eventually binary).

## Rank One Constraint System

### Overview 

- Plain text R1CS defined by 4 files (3 for matrices `a, b` and `c` and 1 for problem size). Each row of matrix files has three space separated integers `c r v` denoting that the non-zero value `v` appears at row `r` and column `c`. Each matrix file **must end with a blank line**!
- JSON R1CS defined as having *constraints* key referring to a list whose rth entry is a triple of key-value pairs denoting the column-values of the rth row of matrices `a, b and c` respectively. See below for examples.
   
### Text

Should consist of 4 files; One for each of matrix `a, b` and `c` and one for the problem size.

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
 
 #### Witness data

Witness data represents the vector `z = [1 | public | private]` in the matrix equation Az &#8857; Bz = Cz where &#8857; denotes the  [*Hadamard product*](https://en.wikipedia.org/wiki/Hadamard_product_(matrices\))
is read from two separate files `filePath/public` and `filePath/aux` which should be line separated integers having the same number of lines as declared in the `.problem_size` file from above

In the example above, valid witness information would be

|**public**|**aux**|
|----------|-------|
| 1        |  1    |
| 0        |  1    |
| 1        |       |

to denote the vector `z^t = [1, 0, 1, 1, 1]`. Note that 1 is assumed to be the first entry of the data which is taken from the first value os public

At this moment, in our profiling we have temporarily reversed the order of information (based on libsnark output which appears to reverse the role of public and private information on the level of the matrix indexing. 
This means that the above example would be interpreted as `z^t = [1, 1, 1, 0, 1] = [1 | private | public]`

### JSON

The JSON file format of a R1CS (with inputs) consists of 4 keys: `header`, `primary_inputs, aux_input` and `constraints` each having list values as demonstrated in the following example:

```python
{
  "header": [2, 3],
  "primary_input": ["1", "0"],
  "aux_input": ["1", "1", "1"],
  "constraints": [
    [{"1": 1, "2": 1}, {"0": 1}, {"2": 1}],
    [{"2": 1}, {"3": 1}, {"3": 1}],
    [{"1": 1, "2": 1, "3": 1}, {"1": 1, "2": 1, "3": 1}, {"4": 1}]
  ]
}
```

The header is used to describe the number of primary and auxiliary inputs respectively (in the event that primary and auxiliary inputs are not supplied.)

where the constraint at index `r` denotes the non-zero entries of the r-th row of matrices `a, b` and `c` respectively. 

- Constraint values can be either of type String or Integer.

In the event that we are only interested in the constraints (without the inputs), this requirements reduces to

```python
{
  "header": [2, 3],
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


