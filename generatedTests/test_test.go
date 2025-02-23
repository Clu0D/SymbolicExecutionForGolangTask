package main

import (
)

func test_TestAssume() {

}

func test_TestMakeSymbolic() {

	// Test case 1
	1170@param#a := 1
	1171@makeSymbolic# := 0
    result := TestMakeSymbolic(1170@param#a)
	if result != 0 {
	    t.Errorf("test_TestMakeSymbolic1 failed")
	}
	
	// Test case 2
	1170@param#a := 1
	1171@makeSymbolic# := 1
    result := TestMakeSymbolic(1170@param#a)
	if result != 42 {
	    t.Errorf("test_TestMakeSymbolic2 failed")
	}
	
}

func main() {
	test_TestAssume()
	test_TestMakeSymbolic()
}

// Original code:

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

