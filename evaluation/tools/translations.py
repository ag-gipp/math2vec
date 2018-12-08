
forward_translation_dict = {
    'p_{i}': 'math-p',
    'u_{g}': 'math-u',
    '\\mu': 'math-mu',
    '\\boldsymbol{F}_{r}': 'math-f',
    '\\dot{\\mathbf{x}}': 'math-x',
    '\\psi': 'math-psi',
    '\\mathrm{rpm}_{\\text{motor}}': 'math-rpm',
    '\\dot{q_{i}}': 'math-q',
    '\\delta _{\\nu}^{\\mu}': 'math-delta',
    'X_{1}': 'math-x',
    '\\sigma': 'math-sigma',
    '\\Theta': 'math-theta',
    '\\mu_{x}': 'math-mu',
    'e_{1}': 'math-e',
    'E_{\\mathrm{k}}': 'math-e',
    'E_{K}': 'math-e',
    'a_{i}': 'math-a',
    '\\varepsilon': 'math-varepsilon',
    '\\beta': 'math-beta',
    'Q_{1}': 'math-q',
    '\\mathrm{e}': 'math-e',
    '\\sigma_{x}': 'math-sigma',
    'r_{ij}': 'math-r',
    'q_{i}': 'math-q',
    '\\alpha_{1}': 'math-alpha',
    'H_{B}': 'math-h',
    'u^{\\alpha}': 'math-u',
    '\\Psi': 'math-psi',
    '\\mathbf{m}_{1}': 'math-m',
    '\\tau': 'math-tau',
    '\\pi_{i}': 'math-pi',
    'h_{r,s}': 'math-h',
    'h_{i}': 'math-h',
    '\\boldsymbol{s}': 'math-s',
    'p_{1}': 'math-p',
    'f_{0}': 'math-f',
    'x_{0}': 'math-x',
    '\\Phi': 'math-phi',
    '\\omega_{k}': 'math-omega',
    '\\Sigma': 'math-sigma',
    'R_{\\text{s normal}}': 'math-r',
    'S_{i-1}': 'math-s',
    'p_{y}': 'math-p',
    'k_{1}': 'math-k',
    '\\dot{u}_{x}': 'math-u',
    '\\lambda_{in}': 'math-lambda',
    'P_{x}': 'math-p',
    'V_{1}': 'math-v',
    'f_{c}': 'math-f',
    'g_{j}': 'math-g',
    'E_{\\mathrm{r}}': 'math-e',
    '\\lambda': 'math-lambda',
    'x^{\\nu}': 'math-x',
    'V_{x}': 'math-v',
    'C_{i}': 'math-c',
    '\\pi': 'math-pi',
    "S'": "math-s",
    '\\lambda_{1}': 'math-lambda',
    's_{V}': 'math-s',
    '\\mathcal{F}': 'math-f',
    'i_{2}': 'math-i',
    'u_{i}': 'math-u',
    '\\sigma_{y}': 'math-sigma',
    '\\mathcal{R}': 'math-r',
    '\\Gamma_{\\infty}': 'math-gamma',
    'p_{n}': 'math-p',
    'T_{c}': 'math-t',
    'V_{2}': 'math-v',
    '\\mu_{0}': 'math-mu',
    '\\alpha': 'math-alpha',
    '\\omega_{i}': 'math-omega',
    '\\theta': 'math-theta',
    'I_{c}': 'math-i',
    'u^{\\beta}': 'math-u',
    '\\hat{\\sigma}': 'math-sigma',
    '\\Pi_{n}': 'math-pi',
    '\\Gamma': 'math-gamma',
    '\\eta': 'math-eta',
    '\\mu_{y}': 'math-mu',
    '\\omega': 'math-omega',
    'u_{x}': 'math-u',
    'v_{1}': 'math-v',
    'T_{8}': 'math-t',
    'i_{1}': 'math-i',
    'u^{\\mu}': 'math-u',
    '\\mathbb{Z}': 'math-z',
    'K_{X}': 'math-k',
    'E_{j}': 'math-e',
    'Q_{2}': 'math-q',
    'x_{7}': 'math-x',
    '\\mathrm{seqs}': 'math-seqs',
    'p_{x}': 'math-p',
    'T_{\\alpha\\beta}': 'math-t',
    '\\delta': 'math-delta',
    'X_{i}': 'math-x',
    'h_{-2}': 'math-h',
    '\\ell': 'math-ell',
    '\\mathsf{fv}': 'math-fv',
    '\\phi_{1}': 'math-phi',
    'u_{\\nu}': 'math-u',
    'X_{2}': 'math-x',
    'u_{1}': 'math-u',
    'f^{\\mu}': 'math-f',
    '\\varepsilon_{t}': 'math-varepsilon',
    '\\mathbf{x}': 'math-x',
    'b_{3}': 'math-b',
    'g_{x}': 'math-g',
    'f_{x}': 'math-f',
    '\\mathbb{R}': 'math-r',
    'z_{t}': 'math-z',
    '\\rho': 'math-rho',
    '\\alpha_{2}': 'math-alpha',
    '\\bar{V}': 'math-v',
    'G_{k,\\sigma}': 'math-g',
    'E_{\\mathrm{t}}': 'math-e',
    'z_{1}': 'math-z',
    'P_{1}': 'math-p',
    'y_{k}': 'math-y',
    '\\eta_{\\alpha\\beta}': 'math-eta',
    'z_{t-1}': 'math-z',
    'v_{g}': 'math-v',
    'v_{a}': 'math-v',
    'P_{i}': 'math-p'
}

backward_translation_dict = {
    y:x for x, y in forward_translation_dict.items()
}


def forward_trans_contains(word):
    return word in forward_translation_dict


def forward_trans(word):
    if forward_trans_contains(word):
        return forward_translation_dict.get(word)
    else:
        return "math-"+word.lower()


def backward_trans(word):
    if word in backward_translation_dict:
        return backward_translation_dict.get(word)
    else:
        return word[5:]