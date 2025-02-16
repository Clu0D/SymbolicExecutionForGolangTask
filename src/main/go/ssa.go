package main

import (
    "encoding/json"
    "flag"
    "fmt"
    "go/ast"
    "go/parser"
    "go/token"
    "go/types"
    "go/importer"
    "golang.org/x/tools/go/ssa"
    "golang.org/x/tools/go/ssa/ssautil"
    "io"
    "os"
    "bytes"
    "path/filepath"
    "reflect"
)

type NodeData struct {
    ParentF     string          `json:"parentF"`
    Type        string          `json:"type"`
    ID          int             `json:"id"`
}

type ValueNodeData struct {
    NodeData
    Name        string          `json:"name"`
    ValueType   interface{}     `json:"valueType"`
}

type BlockNode struct {
    NodeData
    Instr       []interface{}   `json:"instr"`
}

type FuncNode struct {
    ValueNodeData
    Params      []interface{}   `json:"paramsNull"`
    Body        interface{}     `json:"body"`
//     Params    []*Parameter  // function parameters; for methods, includes receiver
//     	FreeVars  []*FreeVar    // free variables whose values must be supplied by closure
//     	Locals    []*Alloc      // frame-allocated variables of this function
//     	Recover   *BasicBlock   // optional; control transfers here after recovered panic
//     	AnonFuncs []*Function   // anonymous functions (from FuncLit,RangeStmt) directly beneath this one
}

type ParameterNode struct {
    ValueNodeData
}

type TypeNode struct {
    NodeData
    Name        string          `json:"name"`
}

type LinkNode struct {
    NodeData
    LinkId      int             `json:"linkId"`
}

type IfNode struct {
    NodeData
    Cond        interface{}     `json:"cond"`
    Body        interface{}     `json:"body"`
    ElseBody    interface{}     `json:"elseBody"`
}

// type FieldNode struct {
//     NodeData
//     X       string `json:"struct"`
//     Field   int    `json:"field"`
//     Operands []interface{} `json:"operands"`
// }

type BinOpNode struct {
    ValueNodeData
    X           interface{}     `json:"x"`
    Y           interface{}     `json:"y"`
    Op          string          `json:"op"`
}

type ReturnNode struct {
    NodeData
    Results     []interface{}   `json:"results"`
}

type CallNode struct {
    ValueNodeData
    Value       interface{}     `json:"value"`
    Args        []interface{}   `json:"args"`
}

type InvokeNode struct {
    NodeData
    Value       interface{}     `json:"value"`
    Method      string          `json:"method"`
    Args        []interface{}   `json:"args"`
}

type StoreNode struct {
    NodeData
    Addr        interface{}     `json:"addr"`
    Value       interface{}     `json:"value"`
}

// type ValueNode struct {
//     NodeData
//     Name        string          `json:"name"`
//     ValueType   interface{}     `json:"valueType"`
// }

type JumpNode struct {
    NodeData
    Successor   interface{}     `json:"successor"`
}

type UnOpNode struct {
    ValueNodeData
    Op          string          `json:"op"`
    X           interface{}     `json:"x"`
    CommaOk     bool            `json:"commaOk"`
}

type PanicNode struct {
    NodeData
    X           string          `json:"x"`
}

type ConstNode struct {
    ValueNodeData
}

type AllocNode struct {
    ValueNodeData
}

type SliceNode struct {
    ValueNodeData
    X           interface{}     `json:"x"`
//     Low         interface{}     `json:"low"`
    High        interface{}     `json:"high"`
//     Max         interface{}     `json:"max"`
}

type PhiNode struct {
    ValueNodeData
    Edges       []interface{}   `json:"edges"`
    Preds       []interface{}   `json:"preds"`
}

type MakeSliceNode struct {
    ValueNodeData
    Len         interface{}     `json:"len"`
}

type IndexAddrNode struct {
    ValueNodeData
    X           interface{}     `json:"x"`
    Index       interface{}     `json:"index"`
}

type BuiltInNode struct {
    ValueNodeData
}

type FieldAddrNode struct {
    ValueNodeData
    X           interface{}     `json:"x"`
    Field       int             `json:"field"`
}

type ConvertNode struct {
    ValueNodeData
    X           interface{}     `json:"x"`
}

type GlobalNode struct {
    ValueNodeData
}

type ExtractNode struct {
    ValueNodeData
    Index       int             `json:"index"`
}

type MakeInterfaceNode struct {
    ValueNodeData
    X           interface{}     `json:"x"`
}

type UnknownNode struct {
    NodeData
}

// types
type BasicTypeNode struct{
	NodeData
	Name        string          `json:"name"`
}

type TupleTypeNode struct {
    NodeData
}

type StructTypeNode struct {
	NodeData
	Fields      []interface{}   `json:"fields"`
}

type StructFieldTypeNode struct {
	NodeData
	Name        string          `json:"name"`
	ElemType    interface{}     `json:"elemType"`
// 	Tag         string          `json:"tag"`
}

type PointerTypeNode struct {
	NodeData
	ElemType    interface{}     `json:"elemType"`
}

type NamedTypeNode struct {
	NodeData
	Name        string          `json:"name"`
	Underlying  interface{}     `json:"underlying"`
	Methods     []interface{}   `json:"methods"`
}

type SliceTypeNode struct {
    NodeData
    ElemType    interface{}     `json:"elemType"`
}

type SignatureTypeNode struct {
    NodeData
}

type FuncTypeNode struct {
    NodeData
}

type AliasTypeNode struct {
    NodeData
    Rhs         interface{}     `json:"rhs"`
}

type ArrayTypeNode struct {
    NodeData
    ElemType    interface{}     `json:"elemType"`
    Len         int64           `json:"len"`
}

type InterfaceTypeNode struct {
    NodeData
    Methods     []interface{}   `json:"methods"`
    Embedded    []interface{}   `json:"embedded"`
}


func encodeFunction(node *ssa.Function, nodeIDCounter *int, nodeRefs *map[interface{}]int) interface{} {
    id := *nodeIDCounter
    *nodeIDCounter++

    if linkId, found := (*nodeRefs)[node]; found {
        return LinkNode{
            NodeData: NodeData{ParentF: "", Type: "LinkToNode", ID: id},
            LinkId: linkId,
        }
    }

    (*nodeRefs)[node] = id

    if node.Blocks == nil || len(node.Blocks) == 0 {
        return FuncNode{
            ValueNodeData:  ValueNodeData{
                NodeData:   NodeData{ParentF: "", Type: "*ssa.Function", ID: id},
                Name:       node.Name(),
                ValueType:  encodeNode(node.Type(), nodeIDCounter, nodeRefs),
            },
            Params:     encodeParams(node.Params, nodeIDCounter, nodeRefs),
            Body:       nil,
        }
    } else {
        return FuncNode{
            ValueNodeData:  ValueNodeData{
                NodeData:   NodeData{ParentF: "", Type: "*ssa.Function", ID: id},
                Name:       node.Name(),
                ValueType:  encodeNode(node.Type(), nodeIDCounter, nodeRefs),
            },
            Params:     encodeParams(node.Params, nodeIDCounter, nodeRefs),
            Body:       encodeBlock(node.Blocks[0], nodeIDCounter, nodeRefs),
        }
    }
}

func encodeParams(params []*ssa.Parameter, nodeIDCounter *int, nodeRefs *map[interface{}]int) []interface{} {
    var paramNodes []interface{}

    for _, parameter := range params {
        paramNodes = append(paramNodes, encodeParameter(parameter, nodeIDCounter, nodeRefs))
    }

    return paramNodes
}

func encodeParameter(node *ssa.Parameter, nodeIDCounter *int, nodeRefs *map[interface{}]int) interface{} {
    id := *nodeIDCounter
    *nodeIDCounter++

    if linkId, found := (*nodeRefs)[node]; found {
        return LinkNode{
            NodeData: NodeData{ParentF: node.Parent().String(), Type: "LinkToParam", ID: id},
            LinkId: linkId,
        }
    }

    (*nodeRefs)[node] = id

    return ParameterNode{
        ValueNodeData:  ValueNodeData{
            NodeData:   NodeData{ParentF: node.Parent().String(), Type: "*ssa.Parameter", ID: id},
            Name:       node.Name(),
            ValueType:  encodeNode(node.Type(), nodeIDCounter, nodeRefs),
        },
    }
}

// func encodeType(node types.Type, nodeIDCounter *int, nodeRefs *map[interface{}]int) interface{} {
//     id := *nodeIDCounter
//     *nodeIDCounter++
//
//     if linkId, found := (*nodeRefs)[node]; found {
//         return LinkNode{
//             NodeData: NodeData{ParentF: node.Parent().String(), Type: "LinkToEncoded", ID: id},
//             LinkId: linkId,
//         }
//     }
//
//     (*nodeRefs)[node] = id
//
//     return TypeNode{
//         NodeData:   NodeData{ParentF: node.Parent().String(), Type: "*types.Type", ID: id},
//         Name:       node.String(),
//     }
// }

func encodeBlocks(blocks []*ssa.BasicBlock, nodeIDCounter *int, nodeRefs *map[interface{}]int) []interface{} {
    var blockNodes []interface{}

    for _, block := range blocks {
        blockNodes = append(blockNodes, encodeBlock(block, nodeIDCounter, nodeRefs))
    }

    return blockNodes
}

func encodeBlock(node *ssa.BasicBlock, nodeIDCounter *int, nodeRefs *map[interface{}]int) interface{} {
    id := *nodeIDCounter
    *nodeIDCounter++

    if linkId, found := (*nodeRefs)[node]; found {
        return LinkNode{
            NodeData: NodeData{ParentF: node.Parent().String(), Type: "LinkToBlock", ID: id},
            LinkId: linkId,
        }
    }

    (*nodeRefs)[node] = id

    var instrNodes []interface{}
    for _, instr := range node.Instrs {
        instrNodes = append(instrNodes, encodeNode(instr, nodeIDCounter, nodeRefs))
    }

    return BlockNode{
        NodeData:   NodeData{ParentF: node.Parent().String(), Type: "*ssa.BasicBlock", ID: id},
        Instr:      instrNodes,
    }
}

// func encodeValue(node ssa.Value, nodeIDCounter *int, nodeRefs *map[interface{}]int) interface{} {
//     id := *nodeIDCounter
//     *nodeIDCounter++
//
//     if linkId, found := (*nodeRefs)[node]; found {
//         return LinkNode{
//             NodeData: NodeData{ParentF: node.Parent().String(), Type: "LinkToNode", ID: id},
//             LinkId: linkId,
//         }
//     }
//
//     return encodeNode(node, nodeIDCounter, nodeRefs)
// }

func encodeArray(values []ssa.Value, nodeIDCounter *int, nodeRefs *map[interface{}]int) []interface{} {
    result := make([]interface{}, len(values))

    for i, v := range values {
        result[i] = encodeNode(v, nodeIDCounter, nodeRefs)
    }
    return result
}

func encodeOperands(node ssa.Instruction, nodeIDCounter *int, nodeRefs *map[interface{}]int) []interface{} {
    var operands []interface{}
    var rands []*ssa.Value
    for _, value := range node.Operands(rands) {
        encodedValue := encodeNode(value, nodeIDCounter, nodeRefs)

        operands = append(operands, encodedValue)
    }
    return operands
}

func basicKindToString(kind types.BasicKind) string {
    switch kind {
    case types.Bool:
        return "bool"
    case types.Int:
        return "int"
    case types.Int16:
        return "int16"
    case types.Int32:
        return "int32"
    case types.Int64:
        return "int64"
    case types.Uint:
        return "uint"
    case types.Byte:
        return "byte"
    case types.Float32:
        return "float32"
    case types.Float64:
        return "float64"
    case types.Complex128:
        return "complex128"
    case types.String:
        return "string"
    case types.UntypedNil:
        return "UntypedNil"
    default:
        return "Unknown"
    }
}

func encodeFuncType(node *types.Func, nodeIDCounter *int, nodeRefs *map[interface{}]int) interface{} {
    id := *nodeIDCounter
    *nodeIDCounter++

    if linkId, found := (*nodeRefs)[node]; found {
        return LinkNode{
            NodeData: NodeData{ParentF: "", Type: "LinkToFunc", ID: id},
            LinkId: linkId,
        }
    }

    (*nodeRefs)[node] = id

    return FuncTypeNode{
        NodeData:   NodeData{ParentF: "", Type: "*types.Func", ID: id},
    }
}

func encodeType(node types.Type, nodeIDCounter *int, nodeRefs *map[interface{}]int) interface{} {
    id := *nodeIDCounter
    *nodeIDCounter++

    if linkId, found := (*nodeRefs)[node]; found {
        return LinkNode{
            NodeData: NodeData{ParentF: "", Type: "LinkToType", ID: id},
            LinkId: linkId,
        }
    }

    (*nodeRefs)[node] = id

    switch node := node.(type) {
        case *types.Alias:
            return AliasTypeNode{
                NodeData:   NodeData{ParentF: "", Type: "*types.Alias", ID: id},
                Rhs:    encodeType(node.Rhs(), nodeIDCounter, nodeRefs),
            }

        case *types.Basic:
            return BasicTypeNode{
                NodeData:   NodeData{ParentF: "", Type: "*types.Basic", ID: id},
                Name:       basicKindToString(node.Kind()),
            }

        case *types.Struct:
            fields := make([]interface{}, node.NumFields())
            for i := 0; i < node.NumFields(); i++ {
                field := node.Field(i)
                *nodeIDCounter++
                fields[i] = StructFieldTypeNode{
                NodeData:       NodeData{ParentF: "", Type: "Field", ID: *nodeIDCounter},
                    Name:       field.Name(),
                    ElemType:   encodeNode(field.Type(), nodeIDCounter, nodeRefs),
                    // Tag:         node.Tag(i),
                }
                (*nodeRefs)[field] = *nodeIDCounter
            }
            return StructTypeNode{
                NodeData:       NodeData{ParentF: "", Type: "*types.Struct", ID: id},
                Fields:         fields,
            }

        case *types.Pointer:
            return PointerTypeNode{
                NodeData:       NodeData{ParentF: "", Type: "*types.Pointer", ID: id},
                ElemType:       encodeNode(node.Elem(), nodeIDCounter, nodeRefs),
            }

        case *types.Slice:
            return SliceTypeNode{
                NodeData:   NodeData{ParentF: "", Type: "*types.Slice", ID: id},
                ElemType:       encodeNode(node.Elem(), nodeIDCounter, nodeRefs),
            }

        case *types.Array:
            return ArrayTypeNode{
                NodeData:       NodeData{ParentF: "", Type: "*types.Array", ID: id},
                ElemType:       encodeNode(node.Elem(), nodeIDCounter, nodeRefs),
                Len:            node.Len(),
            }

        case *types.Signature:
            //             var params, results []VarNode
            //             if tParams := node.Params(); tParams != nil {
            //                 params = encodeTuple(tParams, nodeIDCounter, nodeRefs)
            //             }
            //             if tResults := node.Results(); tResults != nil {
            //                 results = encodeTuple(tResults, nodeIDCounter, nodeRefs)
            //             }
            return SignatureTypeNode{
                NodeData:  NodeData{ParentF: "", Type: "*types.Signature", ID: id},
                //                 Params:    params,
                //                 Results:   results,
                //                 Variadic:  node.Variadic(),
            }

        case *types.Interface:
            methods := make([]interface{}, node.NumExplicitMethods())
            for i := 0; i < node.NumExplicitMethods(); i++ {
                method := node.ExplicitMethod(i)
                methods[i] = encodeNode(method, nodeIDCounter, nodeRefs)
            }

            embedded := make([]interface{}, node.NumEmbeddeds())
            for i := 0; i < node.NumEmbeddeds(); i++ {
                embedded[i] = encodeNode(node.EmbeddedType(i), nodeIDCounter, nodeRefs)
            }

            return InterfaceTypeNode{
                NodeData:       NodeData{ParentF: "", Type: "*types.Interface", ID: id},
                Methods:        methods,
                Embedded:       embedded,
            }

        case *types.Tuple:
            return TupleTypeNode{
                NodeData:   NodeData{ParentF: "", Type: "*types.Tuple", ID: id},
            }

        case *types.Named:
            underlying := encodeNode(node.Underlying(), nodeIDCounter, nodeRefs)
            methods := make([]interface{}, node.NumMethods())
            for i := 0; i < node.NumMethods(); i++ {
                method := node.Method(i)
                methods[i] = encodeNode(method, nodeIDCounter, nodeRefs)
            }

            return NamedTypeNode{
                NodeData:   NodeData{ParentF: "", Type: "*types.Named", ID: id},
                Name:       node.Obj().Name(),
                Underlying: underlying,
                Methods:    methods,
            }

        default:
            t := reflect.TypeOf(node)
            fmt.Println("Type was not encoded:", t)
            if stringer, ok := node.(fmt.Stringer); ok {
                fmt.Println("stringer:", stringer.String())
            }

            return UnknownNode{
                NodeData:   NodeData{ParentF: "", Type: "UnknownType", ID: id},
            }
    }
}

func encodeNode(node interface{}, nodeIDCounter *int, nodeRefs *map[interface{}]int) interface{} {
    switch node := node.(type) {
        case *ssa.Function:
            return encodeFunction(node, nodeIDCounter, nodeRefs)
        case *ssa.BasicBlock:
            return encodeBlock(node, nodeIDCounter, nodeRefs)
        case *types.Func:
            return encodeFuncType(node, nodeIDCounter, nodeRefs)
        case types.Type:
            return encodeType(node, nodeIDCounter, nodeRefs)
    }
    id := *nodeIDCounter
    *nodeIDCounter++

    if linkId, found := (*nodeRefs)[node]; found {
        return LinkNode{
            NodeData: NodeData{ParentF: "", Type: "LinkToNode", ID: id},
            LinkId: linkId,
        }
    }

    (*nodeRefs)[node] = id

    switch node := node.(type) {
        case *ssa.If:
            return IfNode{
                NodeData:   NodeData{ParentF: node.Parent().String(), Type: "*ssa.If", ID: id},
                Cond:       encodeNode(node.Cond, nodeIDCounter, nodeRefs),
                Body:       encodeBlock(node.Block().Succs[0], nodeIDCounter, nodeRefs),
                ElseBody:   encodeBlock(node.Block().Succs[1], nodeIDCounter, nodeRefs),
            }

        case *ssa.BinOp:
            return BinOpNode{
                ValueNodeData:  ValueNodeData{
                    NodeData:   NodeData{ParentF: node.Parent().String(), Type: "*ssa.BinOp", ID: id},
                    Name:       node.Name(),
                    ValueType:  encodeNode(node.Type(), nodeIDCounter, nodeRefs),
                },
                X:          encodeNode(node.X, nodeIDCounter, nodeRefs),
                Y:          encodeNode(node.Y, nodeIDCounter, nodeRefs),
                Op:         node.Op.String(),
            }

        case *ssa.Return:
            return ReturnNode{
                NodeData:   NodeData{ParentF: node.Parent().String(), Type: "*ssa.Return", ID: id},
                Results:    encodeArray(node.Results, nodeIDCounter, nodeRefs),
            }

        case *ssa.Call:
            if node.Call.Method == nil {
                return CallNode{
                    ValueNodeData:  ValueNodeData{
                        NodeData:   NodeData{ParentF: node.Parent().String(), Type: "*ssa.Call", ID: id},
                        Name:       node.Name(),
                        ValueType:  encodeNode(node.Type(), nodeIDCounter, nodeRefs),
                    },
                    Value:      encodeNode(node.Call.Value, nodeIDCounter, nodeRefs),
                    Args:       encodeArray(node.Call.Args, nodeIDCounter, nodeRefs),
                }
            } else {
                return InvokeNode{
                    NodeData:   NodeData{ParentF: node.Parent().String(), Type: "*ssa.Call invoke", ID: id},
                    Value:      encodeNode(node.Call.Value, nodeIDCounter, nodeRefs),
                    Method:     node.Call.Method.String(),
                    Args:       encodeArray(node.Call.Args, nodeIDCounter, nodeRefs),
                }
            }

        case *ssa.Store:
            return StoreNode{
                NodeData:   NodeData{ParentF: node.Parent().String(), Type: "*ssa.Store", ID: id},
                Addr:       encodeNode(node.Addr, nodeIDCounter, nodeRefs),
                Value:      encodeNode(node.Val, nodeIDCounter, nodeRefs),
            }

        case *ssa.Jump:
            return JumpNode{
                NodeData:       NodeData{ParentF: node.Parent().String(), Type: "*ssa.Jump", ID: id},
                Successor:      encodeBlock(node.Block().Succs[0], nodeIDCounter, nodeRefs),
            }

        case *ssa.UnOp:
            return UnOpNode{
                ValueNodeData:  ValueNodeData{
                    NodeData:   NodeData{ParentF: node.Parent().String(), Type: "*ssa.UnOp", ID: id},
                    Name:       node.Name(),
                    ValueType:  encodeNode(node.Type(), nodeIDCounter, nodeRefs),
                },
                Op:         node.Op.String(),
                X:          encodeNode(node.X, nodeIDCounter, nodeRefs),
                CommaOk:    node.CommaOk,
            }

        case *ssa.Panic:
            return PanicNode{
                NodeData:   NodeData{ParentF: node.Parent().String(), Type: "*ssa.Panic", ID: id},
                X:          node.X.String(),
            }

        case *ssa.Const:
            return ConstNode{
                ValueNodeData:  ValueNodeData{
                    NodeData:   NodeData{ParentF: "", Type: "*ssa.Const", ID: id},
                    Name:       node.Name(),
                    ValueType:  encodeNode(node.Type(), nodeIDCounter, nodeRefs),
                },
            }
        case *ssa.Alloc:
            return AllocNode{
                ValueNodeData:  ValueNodeData{
                    NodeData:   NodeData{ParentF: node.Parent().String(), Type: "*ssa.Alloc", ID: id},
                    Name:       node.Name(),
                    ValueType:  encodeNode(node.Type(), nodeIDCounter, nodeRefs),
                },
            }
        case *ssa.Slice:
            if node.Low != nil || node.Max != nil {
                panic("TODO")
            }
            if node.High == nil {
                return SliceNode{
                    ValueNodeData:  ValueNodeData{
                        NodeData:   NodeData{ParentF: node.Parent().String(), Type: "*ssa.Slice", ID: id},
                        Name:       node.Name(),
                        ValueType:  encodeNode(node.Type(), nodeIDCounter, nodeRefs),
                    },
                    X:      encodeNode(node.X, nodeIDCounter, nodeRefs),
                    High:   nil,
                }
            } else {
                return SliceNode{
                    ValueNodeData:  ValueNodeData{
                        NodeData:   NodeData{ParentF: node.Parent().String(), Type: "*ssa.Slice", ID: id},
                        Name:       node.Name(),
                        ValueType:  encodeNode(node.Type(), nodeIDCounter, nodeRefs),
                    },
                    X:      encodeNode(node.X, nodeIDCounter, nodeRefs),
                    High:   encodeNode(node.High, nodeIDCounter, nodeRefs),
                }
            }
        case *ssa.Phi:
            return PhiNode{
                ValueNodeData:  ValueNodeData{
                    NodeData:   NodeData{ParentF: node.Parent().String(), Type: "*ssa.Phi", ID: id},
                    Name:       node.Name(),
                    ValueType:  encodeNode(node.Type(), nodeIDCounter, nodeRefs),
                },
                Edges:      encodeArray(node.Edges, nodeIDCounter, nodeRefs),
                Preds:      encodeBlocks(node.Block().Preds, nodeIDCounter, nodeRefs),
            }
        case *ssa.MakeSlice:
            return MakeSliceNode{
                ValueNodeData:  ValueNodeData{
                    NodeData:   NodeData{ParentF: node.Parent().String(), Type: "*ssa.MakeSlice", ID: id},
                    Name:       node.Name(),
                    ValueType:  encodeNode(node.Type(), nodeIDCounter, nodeRefs),
                },
                Len:        encodeNode(node.Len, nodeIDCounter, nodeRefs),
            }
        
        case *ssa.IndexAddr:
            return IndexAddrNode{
                ValueNodeData:  ValueNodeData{
                    NodeData:   NodeData{ParentF: node.Parent().String(), Type: "*ssa.IndexAddr", ID: id},
                    Name:       node.Name(),
                    ValueType:  encodeNode(node.Type(), nodeIDCounter, nodeRefs),
                },
                X:          encodeNode(node.X, nodeIDCounter, nodeRefs),
                Index:      encodeNode(node.Index, nodeIDCounter, nodeRefs),
            }
        case *ssa.Builtin:
            return BuiltInNode{
                ValueNodeData:  ValueNodeData{
                    NodeData:   NodeData{ParentF: "", Type: "*ssa.Builtin", ID: id},
                    Name:       node.Name(),
                    ValueType:  encodeNode(node.Type(), nodeIDCounter, nodeRefs),
                },
            }
        case *ssa.FieldAddr:
            return FieldAddrNode{
                ValueNodeData:  ValueNodeData{
                    NodeData:   NodeData{ParentF: node.Parent().String(), Type: "*ssa.FieldAddr", ID: id},
                    Name:       node.Name(),
                    ValueType:  encodeNode(node.Type(), nodeIDCounter, nodeRefs),
                },
                X:              encodeNode(node.X, nodeIDCounter, nodeRefs),
                Field:          node.Field,
            }
        case *ssa.Convert:
            return ConvertNode{
                ValueNodeData:  ValueNodeData{
                    NodeData:   NodeData{ParentF: node.Parent().String(), Type: "*ssa.Convert", ID: id},
                    Name:       node.Name(),
                    ValueType:  encodeNode(node.Type(), nodeIDCounter, nodeRefs),
                },
                X:              encodeNode(node.X, nodeIDCounter, nodeRefs),
            }
        case *ssa.Global:
            return GlobalNode{
                ValueNodeData:  ValueNodeData{
                    NodeData:   NodeData{ParentF: "", Type: "*ssa.Global", ID: id},
                    Name:       node.Name(),
                    ValueType:  encodeNode(node.Type(), nodeIDCounter, nodeRefs),
                },
            }
        case *ssa.Extract:
            return ExtractNode{
                ValueNodeData:  ValueNodeData{
                    NodeData:   NodeData{ParentF: node.Parent().String(), Type: "*ssa.Extract", ID: id},
                    Name:       node.Name(),
                    ValueType:  encodeNode(node.Type(), nodeIDCounter, nodeRefs),
                },
                Index:      node.Index,
            }
        case *ssa.MakeInterface:
            return MakeInterfaceNode{
                ValueNodeData:  ValueNodeData{
                    NodeData:   NodeData{ParentF: node.Parent().String(), Type: "*ssa.MakeInterface", ID: id},
                    Name:       node.Name(),
                    ValueType:  encodeNode(node.Type(), nodeIDCounter, nodeRefs),
                },
                X:      encodeNode(node.X, nodeIDCounter, nodeRefs),
            }

        default:
            t := reflect.TypeOf(node)
            fmt.Println("Node was not encoded:", t)
            if stringer, ok := node.(fmt.Stringer); ok {
                fmt.Println("stringer:", stringer.String())
            }

            return UnknownNode{
                NodeData:   NodeData{ParentF: "", Type: "Unknown", ID: id},
            }
    }
}

func EncodeSSA(program *ssa.Program) ([]byte, error) {
    nodeIDCounter := 1
    nodeRefs := make(map[interface{}]int)

    var functionNodes []interface{}
    for f, _ := range ssautil.AllFunctions(program) { // **Ensure this retrieves proper functions**
        functionNodes = append(functionNodes, encodeFunction(f, &nodeIDCounter, &nodeRefs))
    }

    return json.MarshalIndent(functionNodes, "", "    ")
}

func buildPackage(files []*ast.File, fset *token.FileSet, inputDir string, outputDir string) {
    pkgName := filepath.Base(inputDir)
    pkg, _, err := ssautil.BuildPackage(
        &types.Config{Importer: importer.Default()},
        fset,
        types.NewPackage(filepath.Base(inputDir), ""),
        files,
        ssa.SanityCheckFunctions,
    )
    if err != nil {
        fmt.Printf("Error building package: %v\n", err)
        return
    }

    outputFileName := filepath.Join(outputDir, fmt.Sprintf("%s.json", pkgName))
    fmt.Printf("Output SSA file: %s\n", outputFileName)
    jsonFile, err := os.Create(outputFileName)
    if err != nil {
        fmt.Printf("Error creating file: %v\n", err)
        return
    }
    defer jsonFile.Close()

    jsonBytes, err := EncodeSSA(pkg.Prog)
    if err != nil {
        fmt.Printf("Error encoding program: %v\n", err)
        return
    }

    _, err = io.Copy(jsonFile, bytes.NewReader(jsonBytes))
    if err != nil {
        fmt.Printf("Error writing JSON to file: %v\n", err)
        return
    }
}

func main() {
    inputDirPtr := flag.String("input", "", "Directory containing .go files to parse")
    outputDirPtr := flag.String("output", "", "Directory to save JSON AST files")

    flag.Parse()

    inputDir := *inputDirPtr
    outputDir := *outputDirPtr

    if inputDir == "" || outputDir == "" {
        fmt.Println("Please specify input and output directories using -input and -output flags.")
        flag.Usage()
        return
    }

    err := os.MkdirAll(outputDir, os.ModePerm)
    if err != nil {
        fmt.Printf("Error creating output directory: %v\n", err)
        return
    }

    var files []*ast.File
    fset := token.NewFileSet()
    var localPath = ""

    _ = filepath.Walk(inputDir, func(path string, info os.FileInfo, err error) error {
        if err != nil {
            return err
        }
        if !info.IsDir() && filepath.Ext(path) == ".go" {
            localPath = filepath.Dir(path)
            node, err := parser.ParseFile(fset, path, nil, parser.ParseComments)
            if err != nil {
                fmt.Printf("Error parsing file %s: %v\n", path, err)
                return nil
            }
            files = append(files, node)
            return nil
        }
        if info.IsDir() && len(files) > 0 {
            buildPackage(files, fset, localPath, outputDir)
            files = []*ast.File{}
            fset = token.NewFileSet()
        }
    return nil
    })

    if len(files) > 0 {
        buildPackage(files, fset, localPath, outputDir)
        files = []*ast.File{}
        fset = token.NewFileSet()
    }
}
