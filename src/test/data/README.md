### JSON File Format

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