# re-frame buy2let

A [re-frame](http://day8.github.io/re-frame/) application designed to

- serve as a showcase of some ClojureScript / re-frame concepts I've learned
- invite comments / suggestions as to ClojureScript / re-frame usage & best practices
- serve as an example of a real world ClojureScript / re-frame application
- be useful as a portfolio management tool for a buy-to-let property investor

## Demo

View the [Online Demo](https://wkok.github.io/re-frame-buy2let)

### Notes

- Authentication with your social network will not actually be attempted. Clicking any of the social sign in buttons will merely navigate to the dashboard while in demo mode.
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

No explicit CSS framework is used as I didn't want the code locked in to some opinionated CSS framework. 
As I am not a styling expert by any stretch of the imagination, I instead opted for using semantic html components (generated with [Hiccup](https://github.com/weavejester/hiccup)), stylable using any of the growing list of [Classless CSS themes](https://css-tricks.com/no-class-css-frameworks/)

In this repository & the demo, I used [Marx](https://mblode.github.io/marx/) purely because I liked the general look & feel (& it's blue ;)

In theory, you should just be able to reference any other classless theme in index.html & it should (for the most part) just work, although I only tested it with [Marx](https://mblode.github.io/marx/).

Of course, there are some CSS customizations in the `resources/public/css` folder

## Backend

This repository specifically contains client-side only code. Any data captured when running this application locally in developmemt mode, or when playing with the online demo, is saved in the local [re-frame app db](http://day8.github.io/re-frame/application-state/), which will be lost once you refresh the browser.

You are responsible for providing your own backend (server, database & messaging protocol), & here you are free to choose your favourite stack.

### Protocol

Hooking up your backend to this re-frame app is done by implementing the `Backend` [protocol](https://clojure.org/reference/protocols) which is defined in the namespace: `wkok.buy2let.backend.protocol`

### Effects

Interaction with the backend is accomplished using re-frame [effects](http://day8.github.io/re-frame/Effects/)

Basically, the functions of the `Backend` [protocol](https://clojure.org/reference/protocols) you implement, is called by the application at the right times where interaction with the backend is needed. The map of backend effect(s) you return, will be merged with other application effects (like updating the local re-frame app-db)

#### Example

For example, to implement the persistence of a CRUD item (eg. Property, Charge, etc.) you'll implement the `Backend` [protocol](https://clojure.org/reference/protocols) function `save-crud-fx` and return an effect like

```clojure
(save-crud-fx [_ account-id crud-type id item on-failure]
    {:my-backend/save {:account-id account-id
                       :crud-type crud-type
                       :id id
                       :item item
                       :on-failure on-failure}})
```

Then also register the effect (unless your backend library does this  for you)

```clojure
(re-frame/reg-fx
 :my-backend/save
 (fn [data _]
   ;; Add code here to send the data to the server / database 
    ))
```

### Optimistic Update

It is worth noting that the app assumes an [Optimistic Update](https://purelyfunctional.tv/guide/optimistic-update-in-re-frame/) strategy when persisting someting to the backend.

This means the local app-db will be opportunistically updated, giving instant affirmative feedback to the user on the screen, while the backend action will continue in the background.

This is based on the assumption that 90% of server updates will succeed, so it is mostly unnecessary for the user to have to wait for the server to respond before updating the screen. But this is explained in more detail on [purelyfunctional.tv](https://purelyfunctional.tv/guide/optimistic-update-in-re-frame/)

