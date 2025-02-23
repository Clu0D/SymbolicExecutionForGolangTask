package main

type Test struct{}

func assume(b bool)

func (a *Test) TestAssume(i, j int) int {
	assume(i == j)
	assume(i == 0)
	assume(j == 1)
	return i + j
}

func makeSymbolic(a any) any

func (a *Test) TestMakeSymbolic() int {
    var i int
    j := makeSymbolic(i)
    if j == 1 {
    	return 42
    }
    return 0
}