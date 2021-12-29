# buy2let

![CI](https://github.com/wkok/buy2let/workflows/CI/badge.svg)

A [ClojureScript](https://clojurescript.org/), [re-frame](http://day8.github.io/re-frame/) application designed to

- serve as a showcase of some of the ClojureScript / re-frame concepts I've learned
- invite collaboration as to ClojureScript / re-frame usage & best practices
- serve as an example of a real world ClojureScript / re-frame application
- be potentially useful as a portfolio management tool for a [buy-to-let](https://en.wikipedia.org/wiki/Buy_to_let) property investor (The [Buy2Let Portfolio Manager](https://www.buy2let.app/) online service & mobile app is built from this repository)

## Demo

View the [Online Demo](https://wkok.github.io/buy2let/demo)

### Notes

- The demo stores all data locally in your browser. New data entered will be lost when reloading the page
- Invoices / attachments will not be uploaded / stored on the demo server & therefore won't download when clicking the paperclip

## Development Mode

### Prerequisites

- [npm (NodeJS)](https://nodejs.org/en/)
- [clj (Clojure command line tools)](https://www.clojure.org/guides/deps_and_cli)

### Run application:

```bash
npm install

clj -A:shadow-cljs watch app
```

shadow-cljs will automatically push cljs changes to the browser.

Wait a bit, then browse to [http://localhost:8280](http://localhost:8280)

## Styling

[Material-UI](https://material-ui.com/) React components wrapped with [reagent-material-ui](https://github.com/arttuka/reagent-material-ui) (for ease of use within ClojureScript)

## Backend

This repository specifically contains client-side only code. Any data captured when running this application locally in development mode, or when playing with the online demo, is saved in the local [re-frame app db](http://day8.github.io/re-frame/application-state/), which will be lost once you refresh the browser.

You are responsible for providing your own backend (server, database & messaging protocol), & here you are free to choose your favourite stack.

### Multimethods

Hooking up your backend to this re-frame app is done by implementing various clojure [multimethods](https://clojure.org/reference/multimethods) which is defined in the namespace: `wkok.buy2let.backend.multimethods`

The dispatching function assumes a global variable declared in the host html page. See the example in `resources/public/index.hrml`

Change this global variable to whatever your `defmethod` functions expect for example:

```javascript
let impl = "my-cool-app"
```

Lastly, the namespace defining your `defmethod`'s should be listed in the `:entries` section of the module in shadow-cljs.edn. Replace `wkok.buy2let.backend.demo` with your backend's namespace.

### Effects

Interaction with the backend is accomplished using re-frame [effects](http://day8.github.io/re-frame/Effects/)

Basically, the [multimethods](https://clojure.org/reference/multimethods) you implement, are called by the application at the right times where interaction with the backend is needed. The map of backend effect(s) you return, will be merged with other application effects (like updating the local re-frame app-db)

#### Example

For example, to implement the persistence of a CRUD item (eg. Property, Charge, etc.) you'll implement the [multimethod](https://clojure.org/reference/multimethods) `save-crud-fx` and return an effect like

```clojure
(defmethod save-crud-fx :my-cool-app
  [{:keys [account-id crud-type id item on-error]}]
  {:my-backend/save {:account-id account-id
                     :crud-type crud-type
                     :id id
                     :item item
                     :on-error on-error}})
```

Then, register the effect (unless your backend library does this  for you)

```clojure
(re-frame/reg-fx
 :my-backend/save
 (fn [data _]
   ;; Add code here to send the data to the server / database
    ))
```

### Optimistic Update

It is worth noting that the app assumes an [Optimistic Update](https://purelyfunctional.tv/guide/optimistic-update-in-re-frame/) strategy when persisting something to the backend.

This means the local app-db will be opportunistically updated, giving instant affirmative feedback to the user on the screen, while the backend action will continue in the background.

This is based on the assumption that 90% of server updates will succeed, so it is mostly unnecessary for the user to have to wait for the server to respond before updating the screen. But this is explained in more detail on [purelyfunctional.tv](https://purelyfunctional.tv/guide/optimistic-update-in-re-frame/)

## License

[MIT License](https://choosealicense.com/licenses/mit/)

Copyright (c) 2020 Werner Kok

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
