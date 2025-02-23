package main

type ArrayOfArrays struct{}

func assume(b bool)

func (a *ArrayOfArrays) TestAssume(i, j int) int {
	assume(i == j)
	assume(i == 0)
	assume(j == 1)
	return i + j
}